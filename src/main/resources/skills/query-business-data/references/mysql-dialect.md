# MaxCompute to MySQL 8.4

The Wiki and raw SQL describe MaxCompute/Hive logic, while live queries execute on MySQL 8.4. Preserve business semantics but translate syntax.

## Required Checks

1. Query `information_schema.columns` before using a table.
2. Use database prefixes `ADS`, `DIM`, or `CDM`.
3. Treat partition field `ds` as a normal `VARCHAR(32)` column.
4. Replace `${bizdate}` and other variables with explicit values or a CTE.
5. Never copy `INSERT OVERWRITE`, `LIFECYCLE`, `SET odps.*`, or partition DDL into a live query.

## Common Mappings

| MaxCompute/Hive | MySQL 8.4 |
|---|---|
| `nvl(a, b)` | `coalesce(a, b)` |
| `if(condition, a, b)` | `if(condition, a, b)` or `case when` |
| `substr(x, p, n)` | `substring(x, p, n)` |
| `to_date(x)` | `date(x)` |
| `date_format(x, 'yyyyMMdd')` | `date_format(x, '%Y%m%d')` |
| `from_unixtime(x)` | `from_unixtime(x)` |
| `datediff(a, b)` | `datediff(a, b)` |
| `concat_ws(sep, ...)` | `concat_ws(sep, ...)` |
| `collect_set(x)` | use `group_concat(distinct x)` only when truncation is acceptable |
| `row_number() over (...)` | supported |
| `qualify` | wrap the window query and filter in an outer query |
| `lateral view explode` | use `json_table` only when the stored MySQL value is valid JSON |
| backtick-free table names | use fully qualified `ADS.table_name` |

## Project-Specific Rules

- 轮动专案 and 进攻专案 are aliases.
- For final reward, use `final_reward`, not `estimate_reward`.
- Region dimension uses `lev2_name`; province dimension uses `lev3_name`. Never invent `province_name` or `region_name`.
- `ADS.ads_bi_attack_exemplary_case_target_achieve_df` provides `lev2_name` and `lev3_name`; `ADS.ads_bi_attack_exemplary_case_final_reward_df` does not carry region fields and must be joined for regional reward rollups.
- Before 2026-03, effective scan quantity uses `effective_scan_qty`; from 2026-03 onward use `effective_scan_qty_2`.
- The same version rule applies to `consumer_scan_qty` and `consumer_scan_qty_2`.
- Sales achievement uses terminal monthly-report sales amount; do not substitute outbound amount.
- When aggregating achievement rate across a region, prefer:

```sql
least(sum(sale_amount) / nullif(sum(sale_target), 0), 1)
```

Do not average row-level `sale_achieve_rate` unless the question explicitly asks for an unweighted average.

## Type Differences

- MySQL `DECIMAL(20,2/4)` values are already rounded to the deployed scale.
- Text fields converted from Hive `STRING` are MySQL `TEXT`; do not group by unnecessary wide text fields.
- Empty business identifiers may be `NULL`; use `coalesce(shop_code, chainstore_code)` only when the table grain defines those fields as mutually exclusive.
