package org.geoserver.metadata.data.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceHolderUtil {

    private PlaceHolderUtil() {}

    private static final Pattern PATTERN_PLACEHOLDER = Pattern.compile("\\$\\{([^\\}]*)\\}");

    public static String replacePlaceHolders(String pattern, Map<String, String> map) {
        Matcher matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        while (matcher.find()) {
            pattern =
                    pattern.substring(0, matcher.start())
                            + map.get(matcher.group(1))
                            + pattern.substring(matcher.end());
            matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        }
        return pattern;
    }

    public static Map<String, String> reversePlaceHolders(String pattern, String value) {
        if (pattern == null) {
            pattern = "";
        }
        Matcher matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group(1));
            pattern =
                    pattern.substring(0, matcher.start())
                            + "\\E(.*)\\Q"
                            + pattern.substring(matcher.end());
            matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        }
        matcher = Pattern.compile("\\Q" + pattern + "\\E").matcher(value == null ? "" : value);
        if (matcher.matches()) {
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                map.put(list.get(i), matcher.group(i + 1));
            }
            return map;
        } else {
            return null;
        }
    }

    public static String getPlaceHolder(String pattern) {
        Matcher matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static List<String> replacePlaceHolder(String pattern, Map<String, List<String>> map) {
        Matcher matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        if (matcher.find()) {
            List<String> list = new ArrayList<>();
            List<String> value = map.get(matcher.group(1));
            if (value != null) {
                for (int i = 0; i < value.size(); i++) {
                    if (value.get(i) == null) {
                        list.add(null);
                    } else {
                        list.add(
                                pattern.substring(0, matcher.start())
                                        + value.get(i)
                                        + pattern.substring(matcher.end()));
                    }
                }
            }
            return list;
        } else {
            return null;
        }
    }
}
