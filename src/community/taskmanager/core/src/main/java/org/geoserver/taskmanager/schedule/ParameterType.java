/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import com.google.common.collect.Lists;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * A Parameter Type For a Task
 *
 * @author Niels Charlier
 */
public interface ParameterType {

    /** STRING type */
    public ParameterType STRING =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public String parse(String value, List<String> dependsOnRawValues) {
                    return value;
                }
            };

    /** INTEGER type */
    public ParameterType INTEGER =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public Integer parse(String value, List<String> dependsOnRawValues) {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            };

    /** BOOLEAN type */
    public ParameterType BOOLEAN =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return Lists.newArrayList("true", "false");
                }

                @Override
                public Boolean parse(String value, List<String> dependsOnRawValues) {
                    return Boolean.parseBoolean(value);
                }
            };

    /** URI type */
    public ParameterType URI =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public java.net.URI parse(String value, List<String> dependsOnRawValues) {
                    try {
                        if (!value.contains(":")) {
                            value = "file:" + value;
                        }
                        return new java.net.URI(value);
                    } catch (URISyntaxException e) {
                        return null;
                    }
                }
            };

    /** SQL Type */
    public ParameterType SQL =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public String parse(String value, List<String> dependsOnRawValues) {
                    // protection against sneaking in extra statement
                    if (value.contains(";")) {
                        return null;
                    }
                    return value;
                }
            };

    /**
     * List possible values for this parameter (when applicable). Include an empty string if custom
     * value is also allowed.
     *
     * @param dependsOnRawValues raw values of depending parameters.
     * @return list of possible values, null if not applicable.
     */
    public List<String> getDomain(List<String> dependsOnRawValues);

    /**
     * Validate and parse a parameter value for this parameter (at run time).
     *
     * @param value the raw value.
     * @param dependsOnRawValues raw values of depending parameters.
     * @return the parsed value, NULL if the value is invalid.
     */
    public Object parse(String value, List<String> dependsOnRawValues);

    /**
     * Validate a parameter value (at configuration time).
     *
     * @param value the raw value.
     * @param dependsOnRawValues raw values of depending parameters.
     * @return true if the value is considered valid at configuration time (may still be considered
     *     invalid at parse time)
     */
    public default boolean validate(String value, List<String> dependsOnRawValues) {
        return parse(value, dependsOnRawValues) != null;
    }

    /**
     * Returns a list of web actions related to this type
     *
     * @return list of web actions
     */
    public default List<String> getActions() {
        return Collections.emptyList();
    }
}
