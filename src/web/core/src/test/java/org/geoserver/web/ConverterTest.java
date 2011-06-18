package org.geoserver.web;

import java.util.Locale;

import org.apache.wicket.IConverterLocator;
import org.apache.wicket.util.convert.IConverter;

public class ConverterTest extends GeoServerWicketTestSupport {

    public void testConvertEmtpyString() {
        // Wicket forms rely on converters returning null from an empty string conversion
        IConverterLocator locator = tester.getApplication().getConverterLocator();
        IConverter convert = locator.getConverter(Integer.class);
        assertNotNull(convert);
        assertNull(convert.convertToObject("", Locale.getDefault()));
    }
}
