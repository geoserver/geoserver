/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

/**
 * A TextField for {@code java.lang.Double} representations that allows for arbitrary number of
 * decimal places, since the default TextField rounds up doubles to three decimals.
 * 
 * @author groldan
 */
public class DecimalTextField extends TextField<Double> {

    private static final long serialVersionUID = 1L;

    private NumberFormat format;

    private IConverter decimalConverter;

    public DecimalTextField(String id, IModel<Double> model) {
        super(id, model, Double.class);
        format = DecimalFormat.getInstance();
        format.setMaximumFractionDigits(16);

        decimalConverter = new IConverter() {
            private static final long serialVersionUID = 1L;

            @Override
            public String convertToString(Object value, Locale locale) {
                return value == null? null : format.format(value);
            }

            @Override
            public Object convertToObject(String value, Locale locale) {
                if (value == null || value.trim().length() == 0) {
                    return null;
                }
                Number parsed;
                try {
                    parsed = format.parse(value);
                } catch (ParseException e) {
                    error(e.getMessage());
                    return null;
                }
                return Double.valueOf(parsed.doubleValue());
            }
        };
    }

    public void setMaximumFractionDigits(int maximumFractionDigits) {
        format.setMaximumFractionDigits(maximumFractionDigits);
    }

    @Override
    public IConverter getConverter(Class<?> type) {
        return decimalConverter;
    }
}
