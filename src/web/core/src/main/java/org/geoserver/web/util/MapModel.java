/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.util.Map;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

/**
 * A model which backs onto an underlying map.
 *
 * <p>The semantics of this model are similar to {@link org.apache.wicket.model.PropertyModel}
 * except for that expressions map to keys of a map rather than java bean property names.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class MapModel<T> implements IModel<T>, IChainingModel<T> {
    private static final long serialVersionUID = 3122822158252376260L;
    IModel<? extends Map<String, ?>> model;
    String expression;

    public MapModel(Map<String, ? extends Object> map, String expression) {
        this(new MapWrappingModel(map), expression);
    }

    public MapModel(IModel<? extends Map<String, ? extends Object>> model, String expression) {
        this.model = model;
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) model.getObject().get(expression);
    }

    @SuppressWarnings("unchecked")
    public void setObject(T val) {
        ((Map<String, Object>) model.getObject()).put(expression, val);
    }

    public void detach() {
        model.detach();
    }

    private static class MapWrappingModel implements IModel<Map<String, ?>> {
        private static final long serialVersionUID = -1474150801738143281L;

        private Map<String, ?> myMap;

        public MapWrappingModel(Map<String, ?> m) {
            myMap = m;
        }

        public Map<String, ?> getObject() {
            return myMap;
        }

        public void setObject(Map<String, ?> arg0) {}

        public void detach() {}
    }

    public IModel<?> getChainedModel() {
        return null;
    }

    public void setChainedModel(IModel<?> arg0) {}
}
