/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

public class PositionPanel extends Panel {
    private static final long serialVersionUID = -4645368967597125299L;

    private ImageAjaxLink<Object> upLink;
    private ImageAjaxLink<Object> downLink;

    public PositionPanel(
            String id, IModel<BatchElement> model, GeoServerTablePanel<BatchElement> tablePanel) {
        super(id, model);

        BatchElement be = model.getObject();
        Batch batch = be.getBatch();

        upLink =
                new ImageAjaxLink<Object>(
                        "up",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/arrow_up.png")) {
                    private static final long serialVersionUID = -4165434301439054175L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = batch.getElements().indexOf(be);
                        batch.getElements().remove(index);
                        batch.getElements().add(index - 1, be);
                        tablePanel.clearSelection();
                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (batch.getElements().indexOf(be) == 0) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        upLink.getImage()
                .add(
                        new AttributeModifier(
                                "alt", new ParamResourceModel("up", PositionPanel.this)));
        add(upLink);

        downLink =
                new ImageAjaxLink<Object>(
                        "down",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/arrow_down.png")) {
                    private static final long serialVersionUID = -8005026702401617344L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = batch.getElements().indexOf(be);
                        batch.getElements().remove(index);
                        batch.getElements().add(index + 1, be);
                        tablePanel.clearSelection();
                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (batch.getElements().indexOf(be) == batch.getElements().size() - 1) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        downLink.getImage()
                .add(
                        new AttributeModifier(
                                "alt", new ParamResourceModel("down", PositionPanel.this)));
        add(downLink);
    }
}
