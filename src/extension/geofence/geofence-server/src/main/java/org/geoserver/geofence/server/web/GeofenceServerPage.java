/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDNDBehavior;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import wicketdnd.DragSource;
import wicketdnd.DropTarget;
import wicketdnd.Location;
import wicketdnd.Operation;
import wicketdnd.Transfer;
import wicketdnd.theme.WebTheme;

/**
 * GeoFence Server wicket administration UI for GeoServer.
 *
 * @author Niels Charlier
 */
public class GeofenceServerPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -8258166751239553791L;

    private GeofenceRulesModel rulesModel;

    private GeoServerTablePanel<ShortRule> rulesPanel;

    private AjaxLink<Object> remove;

    public GeofenceServerPage() {

        // the add button
        add(new AjaxLink<>("addNew") {
            private static final long serialVersionUID = 8443763075141885559L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new GeofenceRulePage(rulesModel.newRule(), rulesModel));
            }
        });

        // the removal button
        add(
                remove = new AjaxLink<>("removeSelected") {
                    private static final long serialVersionUID = 3581476968062788921L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        rulesModel.remove(rulesPanel.getSelection());
                        target.add(rulesPanel);
                    }
                });
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        // the panel
        add(
                rulesPanel = new GeoServerTablePanel<>("rulesPanel", rulesModel = new GeofenceRulesModel(), true) {

                    private static final long serialVersionUID = -8943273843044917552L;

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<ShortRule> itemModel, Property<ShortRule> property) {

                        if (property == GeofenceRulesModel.BUTTONS) {
                            return new ButtonPanel(id, itemModel.getObject());
                        }

                        return null;
                    }

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        remove.setEnabled(!rulesPanel.getSelection().isEmpty());
                        target.add(remove);
                    }
                });
        rulesPanel.add(new WebTheme());
        rulesPanel.add(new DragSource(Operation.MOVE).drag("tr"));
        rulesPanel.add(
                new DropTarget(Operation.MOVE) {
                    private static final long serialVersionUID = 543875667418965337L;

                    @Override
                    public void onDrop(AjaxRequestTarget target, Transfer transfer, Location location) {
                        if (location == null
                                || !(location.getComponent().getDefaultModel().getObject() instanceof ShortRule)) {
                            return;
                        }
                        ShortRule movedRule = transfer.getData();
                        ShortRule targetRule = (ShortRule)
                                location.getComponent().getDefaultModel().getObject();
                        if (movedRule.getId().equals(targetRule.getId())) {
                            return;
                        }
                        if (movedRule.getPriority() < targetRule.getPriority()) {
                            movedRule.setPriority(targetRule.getPriority() + 1);
                        } else {
                            movedRule.setPriority(targetRule.getPriority());
                        }
                        rulesModel.save(movedRule);
                        doReturn(GeofenceServerPage.class);
                    }
                }.dropCenter("tr"));
        rulesPanel.add(new GeoServerDNDBehavior());
        rulesPanel.setOutputMarkupId(true);
    }

    /** Panel with buttons up, down and edit */
    private class ButtonPanel extends Panel {

        private static final long serialVersionUID = 833648465957566970L;

        private ImageAjaxLink<?> upLink;

        private ImageAjaxLink<?> downLink;

        public ButtonPanel(String id, final ShortRule rule) {
            super(id);
            this.setOutputMarkupId(true);

            upLink = new ImageAjaxLink<>("up", new PackageResourceReference(getClass(), "img/arrow_up.png")) {
                private static final long serialVersionUID = -8179503447106596760L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    rulesModel.moveUp(rule);
                    target.add(rulesPanel);
                }

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    if (rulesModel.canUp(rule)) {
                        tag.put("class", "visibility-visible");
                    } else {
                        tag.put("class", "visibility-hidden");
                    }
                }
            };
            upLink.getImage()
                    .add(new AttributeModifier("alt", new ParamResourceModel("GeofenceServerPage.up", upLink)));
            upLink.setOutputMarkupId(true);
            add(upLink);

            downLink = new ImageAjaxLink<>("down", new PackageResourceReference(getClass(), "img/arrow_down.png")) {
                private static final long serialVersionUID = 4640187752303674221L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    rulesModel.moveDown(rule);
                    target.add(rulesPanel);
                }

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    if (rulesModel.canDown(rule)) {
                        tag.put("class", "visibility-visible");
                    } else {
                        tag.put("class", "visibility-hidden");
                    }
                }
            };
            downLink.getImage()
                    .add(new AttributeModifier("alt", new ParamResourceModel("GeofenceServerPage.down", downLink)));
            downLink.setOutputMarkupId(true);
            add(downLink);

            ImageAjaxLink<?> editLink =
                    new ImageAjaxLink<>("edit", new PackageResourceReference(getClass(), "img/edit.png")) {
                        private static final long serialVersionUID = 4640187752303674221L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            setResponsePage(new GeofenceRulePage(rule, rulesModel));
                        }
                    };
            editLink.getImage()
                    .add(new AttributeModifier("alt", new ParamResourceModel("GeofenceServerPage.edit", editLink)));
            editLink.setOutputMarkupId(true);
            add(editLink);
        }
    }
}
