/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.MetadataMap;

/**
 * A model which backs onto an underlying {@link MetadataMap}
 * <p>
 * The semantics of this model are similar to {@link #PropertyModel} except for that expressions map
 * to keys of a map rather than java bean property names.
 * </p>
 * <p>
 * Closely derived from {@link MapModel}
 * </p>
 * 
 * @author Andrea Aime - Geosolutions
 * @author Justin Deoliveira, The Open Planning Project
 */
@SuppressWarnings("serial")
public class MetadataMapModel implements IModel<Object> {

    protected IModel<?> model;

    protected String expression;

    protected Class<?> target;
    
    protected Serializable value;

    public MetadataMapModel(MetadataMap map, String expression, Class<?> target) {
        this(new MetadataMapWrappingModel(map), expression, target);
    }

    public MetadataMapModel(IModel<?> model, String expression, Class<?> target) {
        this.model = model;
        this.expression = expression;
        this.target = target;
    }

    public Object getObject() {
        if(value == null) {
            value = (Serializable) ((MetadataMap) model.getObject()).get(expression, target);
        }
        return value;
    }

    public void setObject(Object object) {
        value = (Serializable) object;
        ((MetadataMap) model.getObject()).put(expression, (Serializable) object);
    }

    public void detach() {
        model.detach();
    }
    
    public String getExpression() {
        return expression;
    }

    private static class MetadataMapWrappingModel implements IModel<Object> {

        private MetadataMap map;

        public MetadataMapWrappingModel(MetadataMap m) {
            map = m;
        }

        public Object getObject() {
            return map;
        }

        public void setObject(Object arg0) {
        }

        public void detach() {
        }

    }


}
