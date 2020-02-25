/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public final class Utils {

    private static final Logger LOGGER = Logging.getLogger(Utils.class);

    private Utils() {}

    public static void info(Logger logger, String message, Object... messageArguments) {
        logger.info(() -> String.format(message, messageArguments));
    }

    public static void debug(Logger logger, String message, Object... messageArguments) {
        logger.fine(() -> String.format(message, messageArguments));
    }

    public static void error(
            Logger logger, Throwable cause, String message, Object... messageArguments) {
        logger.log(Level.SEVERE, cause, () -> String.format(message, messageArguments));
    }

    public static void checkCondition(
            boolean condition, String failMessage, Object... failMessageArguments) {
        if (!condition) {
            throw exception(failMessage, failMessageArguments);
        }
    }

    public static <T> T withDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static ParamsExtractorException exception(String message, Object... messageArguments) {
        return new ParamsExtractorException(null, message, messageArguments);
    }

    public static ParamsExtractorException exception(
            Throwable cause, String message, Object... messageArguments) {
        return new ParamsExtractorException(cause, message, messageArguments);
    }

    private static final class ParamsExtractorException extends RuntimeException {

        public ParamsExtractorException(
                Throwable cause, String message, Object... messageArguments) {
            super(String.format(message, messageArguments), cause);
        }
    }

    public static <T extends Closeable> void closeQuietly(T closable) {
        try {
            closable.close();
        } catch (Exception exception) {
            Utils.error(LOGGER, exception, "Something bad happen when closing.");
        }
    }

    public static Map<String, String[]> parseParameters(Optional<String> queryString)
            throws UnsupportedEncodingException {
        Map<String, String[]> parameters = new HashMap<>();
        if (!queryString.isPresent()) {
            return parameters;
        }
        final String[] parametersParts = queryString.get().split("&");
        for (String parametersPart : parametersParts) {
            String[] parameterParts = parametersPart.split("=");
            if (parameterParts.length < 2) {
                continue;
            }
            String name = URLDecoder.decode(parameterParts[0], "UTF-8");
            String value = URLDecoder.decode(parameterParts[1], "UTF-8");
            String[] values = parameters.get(name);
            if (values == null) {
                parameters.put(name, new String[] {value});
            } else {
                values = Arrays.copyOf(values, value.length() + 1);
                values[value.length()] = value;
                parameters.put(name, values);
            }
        }
        return parameters;
    }

    public static Map.Entry caseInsensitiveSearch(String key, Map<?, ?> map) {
        if (map != null) {
            for (Map.Entry entry : map.entrySet()) {
                if (entry.getKey() instanceof String
                        && ((String) entry.getKey()).toLowerCase().equals(key.toLowerCase())) {
                    return entry;
                }
            }
        }
        return null;
    }
}
