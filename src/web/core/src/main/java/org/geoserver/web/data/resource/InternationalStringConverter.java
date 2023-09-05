/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.Locale;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.geotools.api.util.InternationalString;
import org.geotools.util.SimpleInternationalString;

/** Converter for {@link InternationalString} objects. */
public class InternationalStringConverter implements IConverter<InternationalString> {
    @Override
    public InternationalString convertToObject(String s, Locale locale) throws ConversionException {
        return new SimpleInternationalString(s);
    }

    @Override
    public String convertToString(InternationalString s, Locale locale) {
        return s.toString();
    }
}
