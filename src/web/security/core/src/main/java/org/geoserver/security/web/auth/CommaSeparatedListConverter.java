/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.ArrayList;
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
        String[] split = value.split("\\s*,\\s*");
        final ArrayList list = new ArrayList<>();
        // Need to return an implementation of List<String> that isn't private.
        // Arrays.asList returns a private inner class inside Arrays that can't be added to the
        // allowed XStream types.
        for (String val : split) {
            list.add(val);
        }
        return list;
    }

    @Override
    public String convertToString(List<String> value, Locale locale) {
        return value.stream().collect(Collectors.joining(", "));
    }
}
