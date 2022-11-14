/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.DateConverter;

/** Component to provide a Label with a Date. */
public class DateLabel extends Label {

    private DateConverter converter;

    /**
     * @param id wicket id.
     * @param model a Date Model.
     * @param format a Date format.
     */
    public DateLabel(String id, IModel<Date> model, String format) {
        super(id, model);
        this.converter =
                new DateConverter() {
                    @Override
                    public DateFormat getDateFormat(Locale locale) {
                        return new SimpleDateFormat(format);
                    }
                };
    }

    @Override
    protected IConverter<?> createConverter(Class<?> type) {
        if (Date.class.isAssignableFrom(type)) return converter;
        return null;
    }
}
