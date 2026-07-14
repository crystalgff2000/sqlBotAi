package com.sqlbot.service.query;

import com.sqlbot.config.QueryBusinessDataProperties;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class QueryIntentService {

    private static final Pattern DATA_QUERY_PATTERN = Pattern.compile(
            "多少|金额|数量|占比|达成率|最新一期|各期|趋势|同比|环比|"
                    + "[Tt][Oo][Pp]\\s*\\d+|排名|最高|最低|"
                    + "大区|省区|门店|连锁系统|连锁|单体店|奖励|扫码|目标达成|轮动|进攻专案",
            Pattern.CASE_INSENSITIVE
    );

    private final QueryBusinessDataProperties properties;

    public QueryIntentService(QueryBusinessDataProperties properties) {
        this.properties = properties;
    }

    public boolean isDataQuery(String question) {
        if (!properties.isEnabled() || question == null || question.isBlank()) {
            return false;
        }
        return DATA_QUERY_PATTERN.matcher(question.trim()).find();
    }
}
