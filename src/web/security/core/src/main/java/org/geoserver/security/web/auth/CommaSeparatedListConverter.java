/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

/**
 * Converts a comma separated string in a list of values
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CommaSeparatedListConverter implements IConverter<List<String>> {
    private static final long serialVersionUID = -2772030358671687777L;

    @Override
    public List<String> convertToObject(String value, Locale locale) throws ConversionException {
        return Arrays.asList(value.split("\\s*,\\s*"));
    }

    @Override
    public String convertToString(List<String> value, Locale locale) {
        return value.stream().collect(Collectors.joining(", "));
    }

}
