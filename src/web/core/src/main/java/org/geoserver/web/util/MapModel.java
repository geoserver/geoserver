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
 * The semantics of this model are similar to {@link #PropertyModel} except for
 * that expressions map to keys of a map rather than java bean property names.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class MapModel implements IModel<Object>, IChainingModel<Object> {
    private static final long serialVersionUID = 3122822158252376260L;
    IModel<?> model;
    String expression;
    
    public MapModel( Map<String,?> map, String expression ) {
        this(new MapWrappingModel(map), expression);
    }
    
    public MapModel(IModel<?> model, String expression){
        this.model = model;
        this.expression = expression;
    }
    
    @SuppressWarnings("unchecked")
    public Object getObject() {
        return ((Map<String,Object>)model.getObject()).get( expression );
    }

    @SuppressWarnings("unchecked")
    public void setObject(Object object) {
        ((Map<String,Object>)model.getObject()).put(expression, object);
    }

    public void detach() {
        model.detach();
    }
    
    private static class MapWrappingModel implements IModel<Object>{
        /**
         * 
         */
        private static final long serialVersionUID = -1474150801738143281L;
        
        private Map<?,?> myMap;
        
        public MapWrappingModel(Map<?,?> m){
            myMap = m;
        }

        public Object getObject() {
            return myMap;
        }

        public void setObject(Object arg0) {
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
