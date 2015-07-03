/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import org.geowebcache.filter.parameters.IntegerParameterFilter;

/**
 * Subform that allows editing of an IntegerParameterFilter
 * @author Kevin Smith, OpenGeo
 *
 */
public class IntegerParameterFilterSubform extends AbstractParameterFilterSubform<IntegerParameterFilter> {


    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    private static final IConverter INTEGER = new IConverter() {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public Integer convertToObject(String value, Locale locale) {
            if(value==null || value.isEmpty()) return null;
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
        public String convertToString(Object value, Locale locale) {
            return Integer.toString((Integer)value);
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
                List<Integer> floats = new ArrayList<Integer>(strings.length);
                
                for(String s: strings) {
                    floats.add((Integer)INTEGER.convertToObject(s, locale));
                }
                return floats;
            }
        }

        @Override
        public String convertToString(Object value, Locale locale) {
            @SuppressWarnings("unchecked")
            List<Integer> floats =  (List<Integer>) value;
            Iterator<Integer> i = floats.iterator();
            StringBuilder sb = new StringBuilder();
            if(i.hasNext()) {
                sb.append(INTEGER.convertToString(i.next(), locale));
            }
            while(i.hasNext()){
                sb.append("\r\n");
                sb.append(INTEGER.convertToString(i.next(), locale));
            }
            return sb.toString();
        }
        
    };
    
    public IntegerParameterFilterSubform(String id,
            IModel<IntegerParameterFilter> model) {
        super(id, model);
        
        final Component defaultValue;
        
        defaultValue = new TextField<String>("defaultValue", new PropertyModel<String>(model, "defaultValue"));
        add(defaultValue);
        
        final TextArea<List<Integer>> values;
        values = new TextArea<List<Integer>>("values", new PropertyModel<List<Integer>>(model, "values")) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            public IConverter getConverter(Class<?> type) {
                return CONVERT;
            }
        };
        add(values);
        
        final Component threshold;
        threshold = new TextField<Integer>("threshold", new PropertyModel<Integer>(model, "threshold")) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            public IConverter getConverter(Class<?> type) {
                return INTEGER;
            }
        };
        add(threshold);
    }

}
