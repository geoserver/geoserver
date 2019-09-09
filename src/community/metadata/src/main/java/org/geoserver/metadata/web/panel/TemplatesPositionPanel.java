/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.web.MetadataTemplateTracker;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

public class TemplatesPositionPanel extends Panel {
    private static final long serialVersionUID = -4645368967597125299L;

    public TemplatesPositionPanel(
            String id,
            IModel<List<MetadataTemplate>> templates,
            MetadataTemplateTracker tracker,
            IModel<MetadataTemplate> model,
            GeoServerTablePanel<MetadataTemplate> tablePanel) {
        super(id, model);
        ImageAjaxLink<Object> upLink =
                new ImageAjaxLink<Object>(
                        "up",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/arrow_up.png")) {
                    private static final long serialVersionUID = -4165434301439054175L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = templates.getObject().indexOf(model.getObject());
                        tracker.switchTemplates(
                                model.getObject(), templates.getObject().get(index - 1));
                        templates.getObject().add(index - 1, templates.getObject().remove(index));
                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (templates.getObject().indexOf(model.getObject()) == 0) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        upLink.getImage()
                .add(
                        new AttributeModifier(
                                "alt", new ParamResourceModel("up", TemplatesPositionPanel.this)));
        add(upLink);

        ImageAjaxLink<Object> downLink =
                new ImageAjaxLink<Object>(
                        "down",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/arrow_down.png")) {
                    private static final long serialVersionUID = -8005026702401617344L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = templates.getObject().indexOf(model.getObject());
                        tracker.switchTemplates(
                                model.getObject(), templates.getObject().get(index + 1));
                        templates.getObject().add(index + 1, templates.getObject().remove(index));

                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (templates.getObject().indexOf(model.getObject())
                                == templates.getObject().size() - 1) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        downLink.getImage()
                .add(
                        new AttributeModifier(
                                "alt",
                                new ParamResourceModel("down", TemplatesPositionPanel.this)));
        add(downLink);
    }
}
