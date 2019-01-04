/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

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

    private DecimalConverter decimalConverter;

    public DecimalTextField(String id, IModel<Double> model) {
        super(id, model, Double.class);
        decimalConverter = new DecimalConverter();
    }

    public void setMaximumFractionDigits(int maximumFractionDigits) {
        decimalConverter.setMaximumFractionDigits(maximumFractionDigits);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (Double.class.isAssignableFrom(type)) {
            return (IConverter<C>) decimalConverter;
        }
        return super.getConverter(type);
    }
}
