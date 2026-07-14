package com.sqlbot.service;

import com.sqlbot.config.WikiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikiSearchServiceTest {

    private WikiSearchService wikiSearchService;

    @BeforeEach
    void setUp() {
        WikiProperties properties = new WikiProperties();
        wikiSearchService = new WikiSearchService(properties);
        wikiSearchService.loadWikiDocuments();
    }

    @Test
    void expandBusinessQueriesReplacesRotationAliases() {
        List<String> queries = wikiSearchService.expandBusinessQueries("中一区下各省区最新一期轮动专案的奖励情况");

        assertTrue(queries.contains("中一区下各省区最新一期轮动专案的奖励情况"));
        assertTrue(queries.stream().anyMatch(q -> q.contains("进攻专案")));
        assertTrue(queries.stream().anyMatch(q -> q.contains("最终奖励")));
    }

    @Test
    void matchesOfflineMarketingDomainForRotationRewardQuestion() {
        assertTrue(wikiSearchService.matchesOfflineMarketingDomain("中一区下各省区最新一期轮动专案的奖励情况"));
        assertFalse(wikiSearchService.matchesOfflineMarketingDomain("集罐导购奖励人数是多少"));
    }

    @Test
    void resolveMandatoryOfflineMarketingPagesIncludesRewardAndRegionAssets() {
        List<String> paths = wikiSearchService.resolveMandatoryOfflineMarketingPages(
                "中一区下各省区最新一期轮动专案的奖励情况");

        assertTrue(paths.contains("pages/references/线下营销域.md"));
        assertTrue(paths.contains("pages/数据资产/线下营销域/指标/进攻专案最终奖励.md"));
        assertTrue(paths.contains("pages/数据资产/线下营销域/表/进攻专案最终奖励表.md"));
        assertTrue(paths.contains("pages/数据资产/线下营销域/表/进攻专案目标达成表.md"));
    }

    @Test
    void searchExpandedFindsAttackProjectMetricPage() {
        List<WikiSearchResult> results = wikiSearchService.searchExpanded(
                "中一区下各省区最新一期轮动专案的奖励情况",
                8);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(result ->
                result.getRelativePath().contains("进攻专案最终奖励")));
    }
}
