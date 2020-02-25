/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.DoubleConverter;

/**
 * A {@link IConverter} for {@code java.lang.Double} representations that allows for arbitrary
 * number of decimal places, since the default TextField rounds up doubles to three decimals. This
 * class will also handle positive and negative infinity symbols
 */
@SuppressWarnings("serial")
public class DecimalConverter extends DoubleConverter {

    int maximumFractionDigits = 16;

    /** Returns the maximum number of fraction digits allowed in the configuration */
    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    public void setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
    }

    @Override
    public Double convertToObject(String value, Locale locale) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        final NumberFormat format = getNumberFormat(locale);
        final DecimalFormatSymbols symbols = ((DecimalFormat) format).getDecimalFormatSymbols();
        if (value.equals(symbols.getNaN())) {
            return Double.valueOf(Double.NaN);
        } else if (value.equals(symbols.getInfinity())) {
            return Double.valueOf(Double.POSITIVE_INFINITY);
        } else if (value.equals("-" + symbols.getInfinity())) {
            return Double.valueOf(Double.NEGATIVE_INFINITY);
        } else {
            return super.convertToObject(value, locale);
        }
    }

    @Override
    protected NumberFormat newNumberFormat(Locale locale) {
        NumberFormat format = DecimalFormat.getInstance();
        format.setMaximumFractionDigits(16);
        return format;
    }
}
