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
 * <p>
 * The semantics of this model are similar to {@link org.apache.wicket.model.PropertyModel} except for
 * that expressions map to keys of a map rather than java bean property names.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class MapModel<T> implements IModel<T>, IChainingModel<T> {
    private static final long serialVersionUID = 3122822158252376260L;
    IModel<Map<String,T>> model;
    String expression;
    
    public MapModel( Map<String,T> map, String expression ) {
        this(new MapWrappingModel<T>(map), expression);
    }
    
    public MapModel(IModel<Map<String,T>> model, String expression){
        this.model = model;
        this.expression = expression;
    }
    
    public T getObject() {
        return model.getObject().get(expression);
    }

    public void setObject(T val) {
        model.getObject().put(expression, val);
    }

    public void detach() {
        model.detach();
    }
    
    private static class MapWrappingModel<T> implements IModel<Map<String,T>>{
        /**
         * 
         */
        private static final long serialVersionUID = -1474150801738143281L;
        
        private Map<String,T> myMap;
        
        public MapWrappingModel(Map<String,T> m){
            myMap = m;
        }

        public Map<String, T> getObject() {
            return myMap;
        }

        public void setObject(Map<String,T> arg0) {
        }

        public void detach() {
        }
        
    }

    public IModel<?> getChainedModel() {
        return null;
    }

    public void setChainedModel(IModel<?> arg0) {
        
    }

}
