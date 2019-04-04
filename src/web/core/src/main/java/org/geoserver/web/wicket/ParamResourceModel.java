/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;

/**
 * A resource model that eases up the setup of a {@link StringResourceModel}. Mostly syntactic
 * sugar, remove once Wicket {@link StringResourceModel} learns the value of compact, usable API
 */
@SuppressWarnings("serial")
public class ParamResourceModel extends org.apache.wicket.model.StringResourceModel {

    public ParamResourceModel(String resourceKey, Component component, Object... resources) {
        super(resourceKey, component);
        setParameters(resources);
    }
}
