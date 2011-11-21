/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web;

import java.util.Arrays;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geotools.coverage.grid.io.OverviewPolicy;

public class WCSAdminPage extends BaseServiceAdminPage<WCSInfo> {
    
    public WCSAdminPage() {
        super();
    }

    public WCSAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public WCSAdminPage(WCSInfo service) {
        super(service);
    }

    protected Class<WCSInfo> getServiceClass() {
        return WCSInfo.class;
    }
    
    protected void build(IModel info, Form form) {
        // overview policy
        form.add(new DropDownChoice("overviewPolicy", Arrays.asList(OverviewPolicy.values()), new OverviewPolicyRenderer()));
        form.add(new CheckBox("subsamplingEnabled"));
        
        // resource limits
        TextField maxInputMemory = new TextField("maxInputMemory");
        maxInputMemory.add(new MinimumValidator(0l));
        form.add(maxInputMemory);
        TextField maxOutputMemory = new TextField("maxOutputMemory");
        maxOutputMemory.add(new MinimumValidator(0l));
        form.add(maxOutputMemory);
    }

    protected String getServiceName(){
        return "WCS";
    }
    
    private class OverviewPolicyRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((OverviewPolicy) object).name(), WCSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((OverviewPolicy) object).name();
        }
    }
        
}
