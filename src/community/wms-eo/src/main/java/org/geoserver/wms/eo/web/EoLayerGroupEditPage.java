/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.data.layergroup.LayerGroupPage;
import org.geoserver.web.wicket.ParamResourceModel;

public class EoLayerGroupEditPage extends EoLayerGroupAbstractPage {

    public static final String GROUP = "group";
    public static final String WORKSPACE = "workspace";

    public EoLayerGroupEditPage(PageParameters parameters) {
        String groupName = parameters.get(GROUP).toString();
        String wsName = parameters.get(WORKSPACE).toOptionalString();

        LayerGroupInfo lg =
                wsName != null
                        ? getCatalog().getLayerGroupByName(wsName, groupName)
                        : getCatalog().getLayerGroupByName(groupName);

        if (lg == null) {
            error(
                    new ParamResourceModel("LayerGroupEditPage.notFound", this, groupName)
                            .getString());
            doReturn(LayerGroupPage.class);
            return;
        }

        initUI(lg);

        if (!isAuthenticatedAsAdmin()) {
            Form f = (Form) get("form");

            // global layer groups only editable by full admin
            if (lg.getWorkspace() == null) {
                // disable all form components but cancel
                f.visitChildren(
                        (component, visit) -> {
                            if (!(component instanceof AbstractLink
                                    && "cancel".equals(component.getId()))) {
                                component.setEnabled(false);
                            }
                            visit.dontGoDeeper();
                        });
                f.get("save").setVisible(false);

                info(new StringResourceModel("globalLayerGroupReadOnly", this, null).getString());
            }

            // always disable the workspace toggle
            f.get("workspace").setEnabled(false);
        }
    }

    protected void onSubmit(LayerGroupInfo lg) {
        getCatalog().save(lg);
        doReturn();
    }
}
