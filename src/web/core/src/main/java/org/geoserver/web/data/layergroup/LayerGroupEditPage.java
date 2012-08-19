/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Edits a layer group
 */
public class LayerGroupEditPage extends AbstractLayerGroupPage {

    public static final String GROUP = "group";
    public static final String WORKSPACE = "workspace";
    
    public LayerGroupEditPage(PageParameters parameters) {
        String groupName = parameters.get(GROUP).toString();
        String wsName = parameters.get(WORKSPACE).toString();
        
        LayerGroupInfo lg = wsName != null ? getCatalog().getLayerGroupByName(wsName, groupName) :  
            getCatalog().getLayerGroupByName(groupName);
        
        if(lg == null) {
            error(new ParamResourceModel("LayerGroupEditPage.notFound", this, groupName).getString());
            doReturn(LayerGroupPage.class);
            return;
        }
        
        initUI(lg);

        if (!isAuthenticatedAsAdmin()) {
            Form f = (Form)get("form");
    
            //global layer groups only editable by full admin
            if (lg.getWorkspace() == null) {
                //disable all form components but cancel
                f.visitChildren(new IVisitor<Component, Void>() {
                    @Override
                    public void component(Component c, IVisit<Void> visit) {
                        if (!(c instanceof AbstractLink && "cancel".equals(c.getId()))) {
                            c.setEnabled(false);
                        }
                        visit.dontGoDeeper();
                    }
                });
                f.get("save").setVisible(false);
                
                info(new StringResourceModel("globalLayerGroupReadOnly", this, null).getString());
            }

            //always disable the workspace toggle
            f.get("workspace").setEnabled(false);
        }
    }
    
    protected void onSubmit() {
        LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();
        
        getCatalog().save( lg );
        doReturn();
    }
    
}
