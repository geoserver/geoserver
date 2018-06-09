/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;

/**
 * Other params form for databases: schema, loose bbox, pk metadata lookup table
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class AdvancedDbParamPanel extends Panel {
    boolean excludeGeometryless = true;
    boolean looseBBox = true;
    String pkMetadata;
    WebMarkupContainer advancedContainer;
    private WebMarkupContainer advancedPanel;

    public AdvancedDbParamPanel(String id, boolean showLooseBBox) {
        super(id);

        // we create a global container in order to update the visibility of the various items
        // at runtime
        //        final WebMarkupContainer basicParams = new WebMarkupContainer("basicParams");
        // basicParams.setOutputMarkupId(true);
        //        add(basicParams);

        // basicParams.add(new CheckBox("excludeGeometryless", new PropertyModel(this,
        // "excludeGeometryless")));
        add(toggleAdvanced());

        advancedContainer = new WebMarkupContainer("advancedContainer");
        advancedContainer.setOutputMarkupId(true);
        advancedPanel = new WebMarkupContainer("advanced");
        advancedPanel.setVisible(false);

        WebMarkupContainer looseBBoxContainer = new WebMarkupContainer("looseBBoxContainer");
        looseBBoxContainer.setVisible(showLooseBBox);
        CheckBox fastBBoxCheck = new CheckBox("looseBBox", new PropertyModel(this, "looseBBox"));
        looseBBoxContainer.add(fastBBoxCheck);
        advancedPanel.add(looseBBoxContainer);

        WebMarkupContainer excludeGeomlessContainer =
                new WebMarkupContainer("excludeGeometrylessContainer");
        excludeGeomlessContainer.setVisible(showLooseBBox);
        CheckBox excludeGeomlessCheck =
                new CheckBox("excludeGeometryless", new PropertyModel(this, "excludeGeometryless"));
        excludeGeomlessContainer.add(excludeGeomlessCheck);
        advancedPanel.add(excludeGeomlessContainer);

        advancedPanel.add(new TextField("pkMetadata", new PropertyModel(this, "pkMetadata")));
        advancedContainer.add(advancedPanel);
        add(advancedContainer);
    }

    Component toggleAdvanced() {
        final AjaxLink advanced =
                new AjaxLink("advancedLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        advancedPanel.setVisible(!advancedPanel.isVisible());
                        target.add(advancedContainer);
                        target.add(this);
                    }
                };
        advanced.add(
                new AttributeModifier(
                        "class",
                        new AbstractReadOnlyModel() {

                            @Override
                            public Object getObject() {
                                return advancedPanel.isVisible() ? "expanded" : "collapsed";
                            }
                        }));
        advanced.setOutputMarkupId(true);
        return advanced;
    }
}
