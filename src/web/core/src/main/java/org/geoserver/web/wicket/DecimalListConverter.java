/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

/**
 * A converter for a list of decimals based on {@link DecimalConverter}
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class DecimalListConverter implements IConverter<List<Double>> {

    private DecimalConverter converter = new DecimalConverter();

    @Override
    public List<Double> convertToObject(String value, Locale locale) throws ConversionException {
        List<Double> result = new ArrayList<>();
        if (value != null && !"-".equals(value.trim())) {
            String[] values = value.split("\\s+");
            for (String s : values) {
                Double v = converter.convertToObject(s, locale);
                if (v != null) {
                    result.add(v);
                }
            }
        }

        return result;
    }

    @Override
    public String convertToString(List<Double> value, Locale locale) {
        if (value == null || value.isEmpty()) {
            return "-";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < value.size(); i++) {
                String str = converter.convertToString(value.get(i), locale);
                sb.append(str);
                if (i < value.size() - 1) {
                    sb.append(" ");
                }
            }
            return sb.toString();
        }
    }

    public void setMaximumFractionDigits(int maximumFractionDigits) {
        converter.setMaximumFractionDigits(maximumFractionDigits);
    }
}
