/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.web;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.geoserver.csw.CSWInfo;
import org.geoserver.web.services.BaseServiceAdminPage;

public class CSWAdminPage extends BaseServiceAdminPage<CSWInfo> {

    
    public CSWAdminPage() {
        super();
    }

    public CSWAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public CSWAdminPage(CSWInfo service) {
        super(service);
    }

    protected Class<CSWInfo> getServiceClass() {
        return CSWInfo.class;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void build(final IModel info, Form form) {
        
        
    }    
    
    protected String getServiceName(){
       return "CSW";
    }
}
