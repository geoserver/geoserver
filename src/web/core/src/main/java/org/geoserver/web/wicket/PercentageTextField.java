/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

/**
 * A TextField for {@code java.lang.Double} representation as a percentage.
 *
 * @author Jody Garnett (Boundless)
 */
public class PercentageTextField extends TextField<Double> {
    private static final long serialVersionUID = -4589385113632745745L;

    private int maximumFractionDigits = 1;

    private IConverter<Double> percentConverter =
            new IConverter<Double>() {
                private static final long serialVersionUID = -8409029711658542273L;

                @Override
                public String convertToString(Double value, Locale locale) {
                    NumberFormat format = formatter(locale);
                    return value == null ? null : format.format(value);
                }

                @Override
                public Double convertToObject(String value, Locale locale) {
                    if (value == null || value.trim().length() == 0) {
                        return null;
                    }
                    if (!value.endsWith("%")) {
                        value += "%";
                    }
                    NumberFormat format = formatter(locale);
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

    public PercentageTextField(String id) {
        super(id, Double.class);
    }

    public PercentageTextField(String id, IModel<Double> model) {
        super(id, model, Double.class);
    }

    private NumberFormat formatter(Locale locale) {
        NumberFormat format = NumberFormat.getPercentInstance(locale);
        format.setMaximumFractionDigits(maximumFractionDigits);

        return format;
    }

    public void setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (Double.class.isAssignableFrom(type)) {
            return (IConverter<C>) percentConverter;
        }
        return super.getConverter(type);
    }
}
