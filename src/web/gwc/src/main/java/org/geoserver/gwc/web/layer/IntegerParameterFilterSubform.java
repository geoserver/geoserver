/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.geowebcache.filter.parameters.IntegerParameterFilter;

/**
 * Subform that allows editing of an IntegerParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public class IntegerParameterFilterSubform extends AbstractParameterFilterSubform<IntegerParameterFilter> {

    @Serial
    private static final long serialVersionUID = 4625052381807389891L;

    private static final IConverter<Integer> INTEGER = new IConverter<>() {

        @Serial
        private static final long serialVersionUID = -998131942023964739L;

        @Override
        public Integer convertToObject(String value, Locale locale) {
            if (value == null || value.isEmpty()) return null;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new ConversionException(ex)
                        .setConverter(this)
                        .setLocale(locale)
                        .setTargetType(Integer.class)
                        .setSourceValue(value)
                        .setResourceKey("notAValidNumber");
            }
        }

        @Override
        public String convertToString(Integer value, Locale locale) {
            return Integer.toString(value);
        }
    };

    private static final IConverter<List<Integer>> CONVERT = new IConverter<>() {
        /** serialVersionUID */
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public List<Integer> convertToObject(String value, Locale locale) {
            if (value == null) {
                return null;
            } else {
                String[] strings = StringUtils.split(value, "\r\n");
                List<Integer> floats = new ArrayList<>(strings.length);

                for (String s : strings) {
                    floats.add(INTEGER.convertToObject(s, locale));
                }
                return floats;
            }
        }

        @Override
        public String convertToString(List<Integer> value, Locale locale) {
            Iterator<Integer> i = value.iterator();
            StringBuilder sb = new StringBuilder();
            if (i.hasNext()) {
                sb.append(INTEGER.convertToString(i.next(), locale));
            }
            while (i.hasNext()) {
                sb.append("\r\n");
                sb.append(INTEGER.convertToString(i.next(), locale));
            }
            return sb.toString();
        }
    };

    public IntegerParameterFilterSubform(String id, IModel<IntegerParameterFilter> model) {
        super(id, model);

        final Component defaultValue = new TextField<>("defaultValue", new PropertyModel<>(model, "defaultValue"));
        add(defaultValue);

        final TextArea<List<Integer>> values = new TextArea<>("values", new PropertyModel<>(model, "values")) {
            @Serial
            private static final long serialVersionUID = 1397063859210766872L;

            @SuppressWarnings("unchecked")
            @Override
            public <S> IConverter<S> getConverter(Class<S> type) {
                if (List.class.isAssignableFrom(type)) {
                    return (IConverter<S>) CONVERT;
                }
                return super.getConverter(type);
            }
        };
        values.setConvertEmptyInputStringToNull(false);
        add(values);

        final Component threshold = new TextField<Integer>("threshold", new PropertyModel<>(model, "threshold")) {
            @Serial
            private static final long serialVersionUID = -3975284862791672686L;

            @SuppressWarnings("unchecked")
            @Override
            public <S> IConverter<S> getConverter(Class<S> type) {
                if (Integer.class.isAssignableFrom(type)) {
                    return (IConverter<S>) INTEGER;
                }
                return super.getConverter(type);
            }
        };
        add(threshold);
    }
}
