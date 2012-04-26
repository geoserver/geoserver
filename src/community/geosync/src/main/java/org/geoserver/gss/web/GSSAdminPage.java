/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.geoserver.gss.config.GSSInfo;
import org.geoserver.web.services.BaseServiceAdminPage;

public class GSSAdminPage extends BaseServiceAdminPage<GSSInfo> {

    public GSSAdminPage() {
        super();
    }

    @Override
    protected Class<GSSInfo> getServiceClass() {
        return GSSInfo.class;
    }

    @Override
    protected String getServiceName() {
        return "GSS";
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void build(IModel/* <GSSInfo> */info, Form form) {

    }

}
