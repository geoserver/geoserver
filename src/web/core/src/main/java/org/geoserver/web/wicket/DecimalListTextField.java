/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.List;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

/**
 * A TextField for a space separated list of {@code java.lang.Double} representations that allows
 * for arbitrary number of decimal places, since the default TextField rounds up doubles to three
 * decimals.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DecimalListTextField extends TextField<List> {

    private static final long serialVersionUID = 1L;

    DecimalListConverter decimalListConverter;

    public DecimalListTextField(String id, IModel<List> model) {
        super(id, model, List.class);
        decimalListConverter = new DecimalListConverter();
        setConvertEmptyInputStringToNull(false);
    }

    public void setMaximumFractionDigits(int maximumFractionDigits) {
        decimalListConverter.setMaximumFractionDigits(maximumFractionDigits);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (List.class.isAssignableFrom(type)) {
            return (IConverter<C>) decimalListConverter;
        }
        return super.getConverter(type);
    }
}
