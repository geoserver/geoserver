/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.wps.WPSInfo;

/**
 * Configure the WPS service global informations
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class WPSAdminPage extends BaseServiceAdminPage<WPSInfo> {
    protected Class<WPSInfo> getServiceClass() {
        return WPSInfo.class;
    }

    protected String getServiceName() {
        return "WPS";
    }

    @Override
    protected void build(IModel info, Form form) {
        TextField connectionTimeout = new TextField("connectionTimeout", Integer.class);
        connectionTimeout.add(new MinimumValidator<Integer>(-1));
        form.add(connectionTimeout);
    }

}
