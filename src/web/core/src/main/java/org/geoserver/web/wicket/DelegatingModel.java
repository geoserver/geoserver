/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public class DelegatingModel<T> implements IModel<T> {
    Component myComponent; 

    public DelegatingModel(Component c){
        myComponent = c;
    }

    public T getObject(){
        return (T) myComponent.getDefaultModel().getObject();
    }

    public void setObject(T o){
        IModel<T> mod = (IModel<T>) myComponent.getDefaultModel(); 
        mod.setObject(o);
    }

    public void detach(){
        myComponent.getDefaultModel().detach();
    }
}


