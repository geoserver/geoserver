/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.io.Serializable;
import org.apache.wicket.ajax.AjaxRequestTarget;

public class AjaxTargetAndModel<T extends Serializable> {

    private T model;
    private AjaxRequestTarget target;

    public AjaxTargetAndModel() {}

    public AjaxTargetAndModel(T model, AjaxRequestTarget target) {
        super();
        this.model = model;
        this.target = target;
    }

    public T getModel() {
        return model;
    }

    public void setModel(T model) {
        this.model = model;
    }

    public AjaxRequestTarget getTarget() {
        return target;
    }

    public void setTarget(AjaxRequestTarget target) {
        this.target = target;
    }
}
