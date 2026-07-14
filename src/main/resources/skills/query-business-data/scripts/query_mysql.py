#!/usr/bin/env python3
# Compatible with Python 3.6+
import argparse
import csv
import json
import re
import subprocess
import sys
import time
from pathlib import Path


FORBIDDEN_WORDS = {
    "ALTER",
    "ANALYZE",
    "CALL",
    "COMMIT",
    "CREATE",
    "DEALLOCATE",
    "DELETE",
    "DO",
    "DROP",
    "EXECUTE",
    "GRANT",
    "HANDLER",
    "INSERT",
    "KILL",
    "LOAD",
    "LOCK",
    "MERGE",
    "OPTIMIZE",
    "PREPARE",
    "PROCEDURE",
    "PURGE",
    "RELEASE",
    "RENAME",
    "REPAIR",
    "REPLACE",
    "RESET",
    "REVOKE",
    "ROLLBACK",
    "SET",
    "SHOW",
    "START",
    "TRUNCATE",
    "UNLOCK",
    "UPDATE",
    "USE",
}

FORBIDDEN_FUNCTIONS = {
    "BENCHMARK",
    "GET_LOCK",
    "IS_FREE_LOCK",
    "IS_USED_LOCK",
    "LOAD_FILE",
    "MASTER_POS_WAIT",
    "RELEASE_ALL_LOCKS",
    "RELEASE_LOCK",
    "SLEEP",
    "SYS_EXEC",
    "SYS_EVAL",
}

FORBIDDEN_SCHEMAS = {"MYSQL", "PERFORMANCE_SCHEMA", "SYS"}


class Token(object):
    __slots__ = ("kind", "value", "depth", "position")

    def __init__(self, kind, value, depth, position):
        self.kind = kind
        self.value = value
        self.depth = depth
        self.position = position


class QueryRejected(ValueError):
    pass


def run_subprocess(command, input_text, timeout_seconds):
    """Run a subprocess with Python 3.6-compatible arguments."""
    return subprocess.run(
        command,
        input=input_text,
        universal_newlines=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=timeout_seconds,
        check=False,
    )


def tokenize(sql):
    tokens = []
    depth = 0
    index = 0
    length = len(sql)
    while index < length:
        char = sql[index]
        if char.isspace():
            index += 1
            continue
        if sql.startswith("--", index) or sql.startswith("/*", index) or char == "#":
            raise QueryRejected("SQL comments are not allowed")
        if char in {"'", '"'}:
            quote = char
            start = index
            index += 1
            while index < length:
                if sql[index] == "\\":
                    index += 2
                    continue
                if sql[index] == quote:
                    if index + 1 < length and sql[index + 1] == quote:
                        index += 2
                        continue
                    index += 1
                    break
                index += 1
            else:
                raise QueryRejected("Unterminated string literal")
            tokens.append(Token("string", sql[start:index], depth, start))
            continue
        if char == "`":
            start = index
            index += 1
            while index < length:
                if sql[index] == "`":
                    if index + 1 < length and sql[index + 1] == "`":
                        index += 2
                        continue
                    index += 1
                    break
                index += 1
            else:
                raise QueryRejected("Unterminated quoted identifier")
            tokens.append(Token("identifier", sql[start:index], depth, start))
            continue
        if char == "(":
            tokens.append(Token("symbol", char, depth, index))
            depth += 1
            index += 1
            continue
        if char == ")":
            depth -= 1
            if depth < 0:
                raise QueryRejected("Unbalanced parentheses")
            tokens.append(Token("symbol", char, depth, index))
            index += 1
            continue
        if char == ";":
            tokens.append(Token("semicolon", char, depth, index))
            index += 1
            continue
        if char == "@":
            raise QueryRejected("MySQL user and system variables are not allowed")
        if char.isalpha() or char == "_":
            start = index
            index += 1
            while index < length and (sql[index].isalnum() or sql[index] in {"_", "$"}):
                index += 1
            tokens.append(Token("word", sql[start:index].upper(), depth, start))
            continue
        if char.isdigit():
            start = index
            index += 1
            while index < length and (sql[index].isdigit() or sql[index] == "."):
                index += 1
            tokens.append(Token("number", sql[start:index], depth, start))
            continue
        tokens.append(Token("symbol", char, depth, index))
        index += 1
    if depth != 0:
        raise QueryRejected("Unbalanced parentheses")
    return tokens


def word_tokens(tokens):
    return [token for token in tokens if token.kind == "word"]


def has_word_sequence(words, sequence):
    values = [token.value for token in words]
    width = len(sequence)
    return any(values[index : index + width] == list(sequence) for index in range(len(values) - width + 1))


def parse_top_level_limit(tokens, max_rows):
    top = [token for token in tokens if token.depth == 0 and token.kind != "semicolon"]
    limit_positions = [index for index, token in enumerate(top) if token.kind == "word" and token.value == "LIMIT"]
    if not limit_positions:
        return None
    if len(limit_positions) > 1:
        raise QueryRejected("Multiple top-level LIMIT clauses are not allowed")
    index = limit_positions[0] + 1
    if index >= len(top) or top[index].kind != "number" or not top[index].value.isdigit():
        raise QueryRejected("LIMIT must use integer literals")
    first = int(top[index].value)
    count = first
    if index + 1 < len(top) and top[index + 1].value == ",":
        if index + 2 >= len(top) or top[index + 2].kind != "number" or not top[index + 2].value.isdigit():
            raise QueryRejected("LIMIT offset,count must use integer literals")
        count = int(top[index + 2].value)
    elif (
        index + 1 < len(top)
        and top[index + 1].kind == "word"
        and top[index + 1].value == "OFFSET"
    ):
        if index + 2 >= len(top) or top[index + 2].kind != "number" or not top[index + 2].value.isdigit():
            raise QueryRejected("LIMIT count OFFSET offset must use integer literals")
    if count > max_rows:
        raise QueryRejected(f"Explicit LIMIT {count} exceeds the maximum {max_rows}")
    return count


def validate_and_bound(sql, max_rows):
    query = sql.strip().lstrip("\ufeff")
    if not query:
        raise QueryRejected("SQL is empty")
    tokens = tokenize(query)
    semicolons = [token for token in tokens if token.kind == "semicolon"]
    if len(semicolons) > 1:
        raise QueryRejected("Multiple statements are not allowed")
    if semicolons and query[semicolons[0].position + 1 :].strip():
        raise QueryRejected("Only one trailing semicolon is allowed")
    query = query[: semicolons[0].position].rstrip() if semicolons else query
    tokens = [token for token in tokens if token.kind != "semicolon"]
    words = word_tokens(tokens)
    if not words or words[0].value not in {"SELECT", "WITH"}:
        raise QueryRejected("Only SELECT or WITH ... SELECT queries are allowed")
    if words[0].value == "WITH" and not any(
        token.value == "SELECT" and token.depth == 0 for token in words
    ):
        raise QueryRejected("WITH query must end in a top-level SELECT")
    forbidden = sorted({token.value for token in words if token.value in FORBIDDEN_WORDS})
    if forbidden:
        raise QueryRejected(f"Forbidden SQL keyword(s): {', '.join(forbidden)}")
    if has_word_sequence(words, ("INTO", "OUTFILE")) or has_word_sequence(words, ("INTO", "DUMPFILE")):
        raise QueryRejected("File output is not allowed")
    if has_word_sequence(words, ("FOR", "UPDATE")) or has_word_sequence(words, ("LOCK", "IN", "SHARE", "MODE")):
        raise QueryRejected("Locking reads are not allowed")
    if has_word_sequence(words, ("FOR", "SHARE")):
        raise QueryRejected("Locking reads are not allowed")
    dangerous_functions = []
    for index, token in enumerate(tokens[:-1]):
        if (
            token.kind == "word"
            and token.value in FORBIDDEN_FUNCTIONS
            and tokens[index + 1].value == "("
        ):
            dangerous_functions.append(token.value)
    if dangerous_functions:
        raise QueryRejected(f"Forbidden function(s): {', '.join(sorted(set(dangerous_functions)))}")
    forbidden_schemas = sorted({token.value for token in words if token.value in FORBIDDEN_SCHEMAS})
    if forbidden_schemas:
        raise QueryRejected(f"Forbidden system schema(s): {', '.join(forbidden_schemas)}")
    explicit_limit = parse_top_level_limit(tokens, max_rows)
    if explicit_limit is None:
        return f"{query}\nLIMIT {max_rows + 1};", False
    return f"{query};", True


def unescape_mysql(value):
    if value == r"\N":
        return None
    result = []
    index = 0
    mapping = {"0": "\0", "b": "\b", "n": "\n", "r": "\r", "t": "\t", "Z": "\x1a"}
    while index < len(value):
        if value[index] == "\\" and index + 1 < len(value):
            next_char = value[index + 1]
            result.append(mapping.get(next_char, next_char))
            index += 2
        else:
            result.append(value[index])
            index += 1
    return "".join(result)


def parse_mysql_tsv(output):
    lines = output.splitlines()
    if not lines:
        return [], []
    columns = [unescape_mysql(value) for value in lines[0].split("\t")]
    rows = []
    for line_number, line in enumerate(lines[1:], start=2):
        values = [unescape_mysql(value) for value in line.split("\t")]
        if len(values) != len(columns):
            raise RuntimeError(
                f"MySQL output width mismatch at line {line_number}: "
                f"expected {len(columns)}, got {len(values)}"
            )
        rows.append(values)
    return columns, rows


def write_outputs(output_dir, payload):
    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / "result.json"
    csv_path = output_dir / "result.csv"
    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    with csv_path.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(payload["columns"])
        writer.writerows(payload["rows"])
    return json_path, csv_path


def main():
    parser = argparse.ArgumentParser(description="Validate and execute a read-only MySQL query over SSH.")
    parser.add_argument("--sql-file", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--host", default="120.48.17.175")
    parser.add_argument("--ssh-user", default="root")
    parser.add_argument("--ssh-identity-file", default="")
    parser.add_argument("--database", default="ADS")
    parser.add_argument("--mysql-config", default="/root/.ai-data-query.cnf")
    parser.add_argument("--max-rows", type=int, default=500)
    parser.add_argument("--timeout-ms", type=int, default=30000)
    parser.add_argument("--local", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if args.max_rows < 1 or args.max_rows > 500:
        raise QueryRejected("--max-rows must be between 1 and 500")
    if not re.fullmatch(r"[A-Za-z0-9.-]+", args.host):
        raise QueryRejected("Invalid SSH host")
    if not re.fullmatch(r"[A-Za-z0-9_-]+", args.ssh_user):
        raise QueryRejected("Invalid SSH user")
    if args.ssh_identity_file:
        identity_path = Path(args.ssh_identity_file).expanduser().resolve()
        if not identity_path.is_file():
            raise QueryRejected(f"SSH identity file not found: {identity_path}")
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_$]*", args.database):
        raise QueryRejected("Invalid MySQL database name")
    if not re.fullmatch(r"/[A-Za-z0-9_./-]+", args.mysql_config):
        raise QueryRejected("Invalid remote MySQL config path")
    if args.timeout_ms < 1000 or args.timeout_ms > 30000:
        raise QueryRejected("--timeout-ms must be between 1000 and 30000")
    sql_path = Path(args.sql_file).resolve()
    sql = sql_path.read_text(encoding="utf-8")
    bounded_sql, explicit_limit = validate_and_bound(sql, args.max_rows)
    if args.dry_run:
        print(bounded_sql)
        return

    mysql_sql = f"SET SESSION MAX_EXECUTION_TIME={args.timeout_ms};\n{bounded_sql}\n"
    if args.local:
        command = [
            "mysql",
            f"--defaults-extra-file={args.mysql_config}",
            f"--database={args.database}",
            "--batch",
            "--default-character-set=utf8mb4",
        ]
        started = time.monotonic()
        completed = run_subprocess(
            command,
            mysql_sql,
            max(15, args.timeout_ms / 1000 + 20),
        )
        elapsed_ms = round((time.monotonic() - started) * 1000)
        if completed.returncode != 0:
            message = completed.stderr.strip() or completed.stdout.strip() or "Unknown MySQL error"
            raise RuntimeError("MySQL query failed: {0}".format(message))
    else:
        remote_command = (
            f"mysql --defaults-extra-file={args.mysql_config} "
            f"--database={args.database} --batch --default-character-set=utf8mb4"
        )
        command = [
            "ssh",
            "-o",
            "BatchMode=yes",
            "-o",
            "ConnectTimeout=10",
        ]
        if args.ssh_identity_file:
            command.extend(["-i", str(Path(args.ssh_identity_file).expanduser().resolve())])
        command.extend([
            f"{args.ssh_user}@{args.host}",
            remote_command,
        ])
        started = time.monotonic()
        completed = run_subprocess(
            command,
            mysql_sql,
            max(15, args.timeout_ms / 1000 + 20),
        )
        elapsed_ms = round((time.monotonic() - started) * 1000)
        if completed.returncode != 0:
            message = completed.stderr.strip() or completed.stdout.strip() or "Unknown MySQL error"
            raise RuntimeError("MySQL query failed: {0}".format(message))

    columns, rows = parse_mysql_tsv(completed.stdout)
    truncated = not explicit_limit and len(rows) > args.max_rows
    if truncated:
        rows = rows[: args.max_rows]
    notice = (
        f"结果超过 {args.max_rows} 行，当前仅展示前 {args.max_rows} 行；汇总分析可能不完整。"
        if truncated
        else None
    )
    payload = {
        "columns": columns,
        "rows": rows,
        "row_count": len(rows),
        "truncated": truncated,
        "notice": notice,
        "max_rows": args.max_rows,
        "elapsed_ms": elapsed_ms,
        "database": args.database,
        "executed_sql": bounded_sql,
    }
    json_path, csv_path = write_outputs(Path(args.output_dir).resolve(), payload)
    print(
        json.dumps(
            {
                "result_json": str(json_path),
                "result_csv": str(csv_path),
                "row_count": len(rows),
                "truncated": truncated,
                "notice": notice,
                "elapsed_ms": elapsed_ms,
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    try:
        main()
    except (QueryRejected, RuntimeError, subprocess.TimeoutExpired) as error:
        print(json.dumps({"error": str(error)}, ensure_ascii=False), file=sys.stderr)
        sys.exit(2)
