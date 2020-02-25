/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

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
import org.geowebcache.filter.parameters.FloatParameterFilter;

/**
 * Subform that allows editing of a StringParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public class FloatParameterFilterSubform
        extends AbstractParameterFilterSubform<FloatParameterFilter> {

    private static final long serialVersionUID = -1715100884515717529L;

    private static final IConverter<Float> FLOAT =
            new IConverter<Float>() {

                private static final long serialVersionUID = 5393727015187736272L;

                @Override
                public Float convertToObject(String value, Locale locale) {
                    if (value == null || value.isEmpty()) return null;
                    try {
                        return Float.parseFloat(value);
                    } catch (NumberFormatException ex) {
                        throw new ConversionException(ex)
                                .setConverter(this)
                                .setLocale(locale)
                                .setTargetType(Float.class)
                                .setSourceValue(value)
                                .setResourceKey("notAValidNumber");
                    }
                }

                @Override
                public String convertToString(Float value, Locale locale) {
                    return Float.toString(value);
                }
            };

    private static final IConverter<List<Float>> CONVERT =
            new IConverter<List<Float>>() {

                private static final long serialVersionUID = 6972092160668131862L;

                @Override
                public List<Float> convertToObject(String value, Locale locale) {
                    if (value == null) {
                        return null;
                    } else {
                        String[] strings = StringUtils.split(value, "\r\n");
                        List<Float> floats = new ArrayList<Float>(strings.length);

                        for (String s : strings) {
                            floats.add((Float) FLOAT.convertToObject(s, locale));
                        }
                        return floats;
                    }
                }

                @Override
                public String convertToString(List<Float> value, Locale locale) {
                    Iterator<Float> i = value.iterator();
                    StringBuilder sb = new StringBuilder();
                    if (i.hasNext()) {
                        sb.append(FLOAT.convertToString(i.next(), locale));
                    }
                    while (i.hasNext()) {
                        sb.append("\r\n");
                        sb.append(FLOAT.convertToString(i.next(), locale));
                    }
                    return sb.toString();
                }
            };

    public FloatParameterFilterSubform(String id, IModel<FloatParameterFilter> model) {
        super(id, model);

        final Component defaultValue;

        defaultValue =
                new TextField<String>(
                        "defaultValue", new PropertyModel<String>(model, "defaultValue"));
        add(defaultValue);

        final TextArea<List<Float>> values;
        values =
                new TextArea<List<Float>>(
                        "values", new PropertyModel<List<Float>>(model, "values")) {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

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

        final Component threshold;
        threshold =
                new TextField<Float>("threshold", new PropertyModel<Float>(model, "threshold")) {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

                    // Want to use non-localized float parsing so we can handle exponential notation
                    @SuppressWarnings("unchecked")
                    @Override
                    public <S> IConverter<S> getConverter(Class<S> type) {
                        if (Float.class.isAssignableFrom(type)) {
                            return (IConverter<S>) FLOAT;
                        }
                        return super.getConverter(type);
                    }
                };
        add(threshold);
    }
}
