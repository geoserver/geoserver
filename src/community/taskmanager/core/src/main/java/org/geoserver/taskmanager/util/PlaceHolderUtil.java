package org.geoserver.taskmanager.util;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.taskmanager.schedule.TaskException;

public class PlaceHolderUtil {

    public interface ObjectTransform {
        String getObjectFromPlaceholder(String placeholder);
    }

    private PlaceHolderUtil() {}

    private static final Pattern PATTERN_PLACEHOLDER = Pattern.compile("\\$\\{([^\\}]*)\\}");

    private static final Pattern OBJECT_PATTERN_PLACEHOLDER = Pattern.compile("\\#\\{([^\\}]*)\\}");

    public static String replacePlaceHolders(String pattern, Map<String, Serializable> map)
            throws TaskException {
        Matcher matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        while (matcher.find()) {
            Serializable value = map.get(matcher.group(1));
            if (value != null) {
                pattern =
                        pattern.substring(0, matcher.start())
                                + value.toString()
                                + pattern.substring(matcher.end());
            } else {
                throw new TaskException("unavailable placeholder: " + matcher.group(1));
            }
            matcher = PATTERN_PLACEHOLDER.matcher(pattern);
        }
        return pattern;
    }

    public static String replaceObjectPlaceHolder(String pattern, ObjectTransform objectTransform) {
        Matcher matcher = OBJECT_PATTERN_PLACEHOLDER.matcher(pattern);
        if (matcher.find()) {
            pattern =
                    pattern.substring(0, matcher.start())
                            + objectTransform.getObjectFromPlaceholder(matcher.group(1)).toString()
                            + pattern.substring(matcher.end());
        }
        return pattern;
    }
}
