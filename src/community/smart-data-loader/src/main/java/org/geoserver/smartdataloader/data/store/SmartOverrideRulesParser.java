package org.geoserver.smartdataloader.data.store;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class SmartOverrideRulesParser {

    private static final String KEY_VALUE_SEPARATOR = ":\t";
    private static final String RULES_SEPARATOR = "|\t";

    public static final SmartOverrideRulesParser INSTANCE = new SmartOverrideRulesParser();

    public Map<String, String> parse(String rulesStr) {
        Map<String, String> rulesMap = new HashMap<>();
        if (StringUtils.isBlank(rulesStr)) {
            return rulesMap;
        }
        String[] rules = rulesStr.split(Pattern.quote(RULES_SEPARATOR));
        for (String rule : rules) {
            String[] keyValue = rule.split(Pattern.quote(KEY_VALUE_SEPARATOR));
            if (keyValue.length == 2) {
                rulesMap.put(StringUtils.trim(keyValue[0]), StringUtils.trim(keyValue[1]));
            }
        }
        return rulesMap;
    }

    public String encode(Map<String, String> rulesMap) {
        if (rulesMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : rulesMap.entrySet()) {
            sb.append(entry.getKey())
                    .append(KEY_VALUE_SEPARATOR)
                    .append(entry.getValue())
                    .append(RULES_SEPARATOR);
        }
        // remove the latest RULES_SEPARATOR
        if (sb.length() > 0) {
            sb.delete(sb.length() - RULES_SEPARATOR.length(), sb.length());
        }
        return sb.toString();
    }
}
