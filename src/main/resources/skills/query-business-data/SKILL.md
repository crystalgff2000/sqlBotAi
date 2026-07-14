---
name: query-business-data
description: Query the AI data asset platform's live MySQL data from natural-language business questions, using the project Wiki for metric definitions, table selection, field versions, lineage, and terminology. Use when the user asks for actual numbers, latest-period results, rankings, trends, distributions, comparisons, target achievement, rewards, or other live data analysis and expects query results, a visualization, and concise conclusions. Do not use for purely conceptual definitions that can be answered from the Wiki without querying data.
---

# Query Business Data

Turn a natural-language business question into a governed MySQL query, validated results, a chart, and concise analysis.

## Required Context

1. Read `AI_data_asset_platform_agent.md`.
2. Read `wiki/pages/index.md`.
3. For data questions, read the matching `wiki/pages/references/<domain>.md`.
4. Prefer the canonical metric asset, then concepts, table assets, SQL summaries, and raw SQL.
5. Treat 轮动专案 and 进攻专案 as aliases for the same project.

## Intent Gate

Use live MySQL only when the user asks for actual data, such as:

- 多少、金额、数量、占比、达成率
- 最新一期、各期、趋势、同比、环比
- Top N、排名、最高、最低
- 按大区、省区、门店、连锁系统或产品系列分组

For definitions, formulas, lineage, or field explanations without a request for actual values, answer from the Wiki and do not query MySQL.

## Query Workflow

### 1. Interpret

Identify the metric, time range, grain, filters, ranking, and expected comparison.

- Resolve “最新一期” with `MAX(data_date)` from the selected governed table.
- Ask one focused clarification when metric, grain, or time ambiguity would materially change the answer.
- State any consequential default, such as using ratio-of-sums instead of averaging row-level rates.

### 2. Route Through Knowledge

Use this order:

1. Domain reference
2. Canonical metric asset
3. Related concepts
4. Governed table assets
5. SQL source summary or raw SQL

If SQL conflicts with prose, follow SQL behavior and report the conflict.

### 3. Inspect Live MySQL Schema

Before querying a business table, query `information_schema.columns` for its current column names, types, and comments. Do not execute MaxCompute or Hive SQL directly against MySQL.

Read [mysql-dialect.md](references/mysql-dialect.md) whenever raw SQL or MaxCompute expressions influence the query.

### 4. Build SQL

- Use MySQL 8.4 syntax and fully qualified table names such as `ADS.table_name`.
- Use only `SELECT` or `WITH ... SELECT`.
- Do not include SQL comments or multiple statements.
- Preserve the metric grain and avoid joins that multiply rows.
- Aggregate measures before joining when one side has a finer grain.
- Use explicit aliases for every returned measure.
- Use a semantic `LIMIT` only for intentional Top N queries. General result caps are added by the executor.

For the pilot domain and the three acceptance questions, read [mvp-questions.md](references/mvp-questions.md).

### 5. Execute Deterministically

Write the generated SQL to a temporary `.sql` file, then run:

```bash
python3 scripts/query_mysql.py \
  --sql-file /absolute/path/query.sql \
  --output-dir /absolute/path/output
```

The executor:

- connects through SSH to `120.48.17.175`;
- uses the server-side root MySQL client configuration;
- rejects non-read-only SQL, comments, dangerous functions, system schemas, and multiple statements;
- enforces a 30-second MySQL execution timeout;
- fetches at most 501 rows and returns at most 500 rows;
- sets `truncated: true` and a user-facing notice when more than 500 rows exist;
- writes `result.json` and `result.csv`.

Never bypass the executor to run generated business SQL.

### 6. Validate Results

Before analysis:

- confirm the returned grain matches the question;
- check row count and `truncated`;
- check nulls, duplicate grouping keys, date coverage, and denominator validity;
- compare totals or group counts when a join was used;
- measure unmatched row and amount coverage for dimension-enrichment joins;
- distinguish “no rows” from a numeric zero.

Do not present a result as complete when `truncated` is true.
Do not silently discard unmatched dimension rows. Keep an explicit “未匹配” group, state its share, and lower confidence when the gap is material.

### 7. Render a Chart

Use the bundled Python runtime returned by `codex_app.load_workspace_dependencies`, then run:

```bash
<bundled-python> scripts/render_chart.py \
  --input /absolute/path/output/result.json \
  --category <dimension-column> \
  --value <numeric-column> \
  --title "<chart-title>" \
  --output /absolute/path/output/chart.png
```

Chart selection:

- time series: `--chart-type line`;
- category comparison or Top N: `--chart-type bar`;
- rates by region: horizontal bar with percentage formatting;
- multiple panels: provide `--facet <column>` and use every generated chart.

Do not chart identifiers as numeric measures. Keep charts to the result grain and label units.

### 8. Return the Answer

Use this order:

1. Direct answer and applied interpretation
2. Compact result table
3. Chart image(s)
4. Two to four evidence-based conclusions
5. Executed SQL
6. Provenance footer

When `truncated` is true, display this prominently:

> 结果超过 500 行，当前仅展示前 500 行；汇总分析可能不完整。

Required footer:

```text
来源层级：semantic_metric / governed_table / raw_sql
依据页面：...
执行表：...
数据范围：...
返回行数：...（是否截断）
置信度：高 / 中 / 低
限制：...
```

Do not expose database passwords, MySQL config contents, or hidden reasoning.
