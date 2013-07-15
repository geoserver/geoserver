/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
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
 * @author Kevin Smith, OpenGeo
 *
 */
public class FloatParameterFilterSubform extends AbstractParameterFilterSubform<FloatParameterFilter> {


    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    private static final IConverter FLOAT = new IConverter() {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public Float convertToObject(String value, Locale locale) {
            if(value==null || value.isEmpty()) return null;
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
        public String convertToString(Object value, Locale locale) {
            return Float.toString((Float)value);
        }
    };
    
    private static final IConverter CONVERT = new IConverter() {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public Object convertToObject(String value, Locale locale) {
            if(value==null) {
                return null;
            } else {
                String[] strings = StringUtils.split(value, "\r\n");
                List<Float> floats = new ArrayList<Float>(strings.length);
                
                for(String s: strings) {
                    floats.add((Float)FLOAT.convertToObject(s, locale));
                }
                return floats;
            }
        }

        @Override
        public String convertToString(Object value, Locale locale) {
            @SuppressWarnings("unchecked")
            List<Float> floats =  (List<Float>) value;
            Iterator<Float> i = floats.iterator();
            StringBuilder sb = new StringBuilder();
            if(i.hasNext()) {
                sb.append(FLOAT.convertToString(i.next(), locale));
            }
            while(i.hasNext()){
                sb.append("\r\n");
                sb.append(FLOAT.convertToString(i.next(), locale));
            }
            return sb.toString();
        }
        
    };
    
    public FloatParameterFilterSubform(String id,
            IModel<FloatParameterFilter> model) {
        super(id, model);
        
        final Component defaultValue;
        
        defaultValue = new TextField<String>("defaultValue", new PropertyModel<String>(model, "defaultValue"));
        add(defaultValue);
        
        final TextArea<List<Float>> values;
        values = new TextArea<List<Float>>("values", new PropertyModel<List<Float>>(model, "values")) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            public IConverter getConverter(Class<?> type) {
                return CONVERT;
            }
        };
        add(values);
        
        final Component threshold;
        threshold = new TextField<Float>("threshold", new PropertyModel<Float>(model, "threshold")) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            // Want to use non-localized float parsing so we can handle exponential notation
            @Override
            public IConverter getConverter(Class<?> type) {
                return FLOAT;
            }
        };
        add(threshold);
    }

}
