/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.StringParameterFilter;

/**
 * Subform that allows editing of a StringParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public class StringParameterFilterSubform
        extends AbstractParameterFilterSubform<StringParameterFilter> {

    private static final long serialVersionUID = -3815153551079914831L;

    private static final IConverter<List<String>> CONVERT =
            new IConverter<List<String>>() {

                private static final long serialVersionUID = -7486127358227242772L;

                @Override
                public List<String> convertToObject(String value, Locale locale) {
                    if (value == null) {
                        return null;
                    } else {
                        String[] strings = StringUtils.split(value, "\r\n");
                        return Arrays.asList(strings);
                    }
                }

                @Override
                public String convertToString(List<String> value, Locale locale) {
                    Iterator<String> i = value.iterator();
                    StringBuilder sb = new StringBuilder();
                    if (i.hasNext()) {
                        sb.append(i.next());
                    }
                    while (i.hasNext()) {
                        sb.append("\r\n");
                        sb.append(i.next());
                    }
                    return sb.toString();
                }
            };

    private Component normalize;

    public StringParameterFilterSubform(String id, IModel<StringParameterFilter> model) {
        super(id, model);

        final Component defaultValue;

        defaultValue =
                new TextField<String>(
                        "defaultValue", new PropertyModel<String>(model, "defaultValue"));
        add(defaultValue);

        final TextArea<List<String>> values;
        values =
                new TextArea<List<String>>(
                        "values", new PropertyModel<List<String>>(model, "values")) {
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

        normalize =
                new CaseNormalizerSubform(
                        "normalize", new PropertyModel<CaseNormalizer>(model, "normalize"));
        add(normalize);
    }
}
