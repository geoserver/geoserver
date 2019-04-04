/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public class DelegatingModel<T> implements IModel<T> {
    private static final long serialVersionUID = -5182376836143803511L;

    Component myComponent;

    public DelegatingModel(Component c) {
        myComponent = c;
    }

    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) myComponent.getDefaultModel().getObject();
    }

    public void setObject(T o) {
        @SuppressWarnings("unchecked")
        IModel<T> mod = (IModel<T>) myComponent.getDefaultModel();
        mod.setObject(o);
    }

    public void detach() {
        myComponent.getDefaultModel().detach();
    }
}
