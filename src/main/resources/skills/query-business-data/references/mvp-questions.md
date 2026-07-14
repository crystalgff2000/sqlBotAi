# Pilot Questions

Use these patterns for the line-marketing pilot. Always inspect the live schema before execution.

## 1. Latest Reward by Region

Question: 最新一期轮动专案各个大区的最终奖励金额是多少？

- Metric: `sum(final_reward)`
- Latest period: `max(data_date)` from the final reward table
- Region source: target achievement table
- Join key: `data_date + politics_code + coalesce(shop_code, chainstore_code)`
- Prevent multiplication: reduce the target table to one region row per join key before joining the product-series-grain reward table
- Coverage check: return unmatched entities and unmatched reward amount; preserve them as `未匹配大区`
- If unmatched reward is material, state that current MySQL dimension coverage is incomplete and lower confidence
- Chart: horizontal bar, sorted descending

## 2. Top 10 Systems and Stores by Period

Question: 各期轮动专案奖励 Top 10 的连锁系统和单体店分别是哪些？

- Metric: `sum(final_reward)`
- Period: each `data_date`
- Split by `terminal_filings_type`
- Entity key:
  - chain system: `chainstore_code`, label `chainstore_name`
  - single store: `shop_code`, label `shop_name`
- Rank: `row_number()` partitioned by `data_date` and filing type
- Filter: rank `<= 10`
- Chart: facet by period and filing type; horizontal Top 10 bars

Before final SQL, query distinct `terminal_filings_type` values. Do not assume the exact single-store label.

## 3. Latest Achievement by Province

Question: 最新一期轮动专案每个省区的目标达成率怎么样？

- Source: target achievement table
- Latest period: `max(data_date)`
- Province: `lev3_name`
- Rate: ratio of sums, `least(sum(sale_amount) / nullif(sum(sale_target), 0), 1)`
- Also return `sum(sale_amount)`, `sum(sale_target)`, and contributing row count for validation
- Exclude or separately label rows with missing province or zero target
- Chart: horizontal percentage bar, sorted descending

## 4. Latest Reward by Province under a Region

Question: 中一区下各省区最新一期轮动专案的奖励情况

- Metric: `sum(final_reward)`
- Latest period: `max(data_date)` from the final reward table
- Region filter: `lev2_name = '中一区'` from the target achievement table
- Province grain: `lev3_name` from the target achievement table
- Reward source: `ADS.ads_bi_attack_exemplary_case_final_reward_df`
- Region/province source: `ADS.ads_bi_attack_exemplary_case_target_achieve_df`
- Join key: `data_date + politics_code + coalesce(shop_code, chainstore_code)`
- Never use `province_name` or `region_name`; governed fields are `lev2_name` and `lev3_name`
- Prevent multiplication: reduce the target table to one region row per join key before joining the product-series-grain reward table
- Chart: horizontal bar by province, sorted descending

## Acceptance Criteria

Every answer must:

- cite `wiki/pages/references/线下营销域.md`;
- cite the canonical metric or governed table asset;
- show the executed MySQL SQL;
- report the latest resolved `data_date`;
- show a chart;
- include two to four conclusions grounded only in returned data;
- display the truncation notice when `result.json.truncated` is true.
