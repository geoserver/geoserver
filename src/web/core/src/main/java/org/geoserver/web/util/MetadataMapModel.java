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
 *
 * <p>The semantics of this model are similar to {@link org.apache.wicket.model.PropertyModel}
 * except for that expressions map to keys of a map rather than java bean property names.
 *
 * <p>Closely derived from {@link MapModel}
 *
 * @author Andrea Aime - Geosolutions
 * @author Justin Deoliveira, The Open Planning Project
 */
@SuppressWarnings("serial")
public class MetadataMapModel<T> implements IModel<T> {

    protected IModel<MetadataMap> model;

    protected String expression;

    protected Class<?> target;

    protected Serializable value;

    public MetadataMapModel(MetadataMap map, String expression, Class<?> target) {
        this(new MetadataMapWrappingModel(map), expression, target);
    }

    public MetadataMapModel(IModel<MetadataMap> model, String expression, Class<?> target) {
        this.model = model;
        this.expression = expression;
        this.target = target;
    }

    @SuppressWarnings("unchecked")
    public T getObject() {
        if (value == null) {
            value = (Serializable) model.getObject().get(expression, target);
        }
        return (T) value;
    }

    public void setObject(T object) {
        value = (Serializable) object;
        model.getObject().put(expression, (Serializable) object);
    }

    public void detach() {
        model.detach();
    }

    public String getExpression() {
        return expression;
    }

    private static class MetadataMapWrappingModel implements IModel<MetadataMap> {

        private MetadataMap map;

        public MetadataMapWrappingModel(MetadataMap m) {
            map = m;
        }

        public MetadataMap getObject() {
            return map;
        }

        public void setObject(MetadataMap arg0) {}

        public void detach() {}
    }
}
