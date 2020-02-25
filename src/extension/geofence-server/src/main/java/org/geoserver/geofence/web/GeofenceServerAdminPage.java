/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.geofence.services.dto.ShortAdminRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import wicketdnd.*;
import wicketdnd.theme.WebTheme;

public class GeofenceServerAdminPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -4321944040817919546L;

    private GeofenceAdminRulesModel rulesModel;

    private GeoServerTablePanel<ShortAdminRule> rulesPanel;

    private AjaxLink<Object> remove;

    public GeofenceServerAdminPage() {

        add(
                new AjaxLink<Object>("addNew") {

                    private static final long serialVersionUID = -4136656891019857299L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(
                                new GeofenceAdminRulePage(rulesModel.newRule(), rulesModel));
                    }
                });

        add(
                remove =
                        new AjaxLink<Object>("removeSelected") {
                            private static final long serialVersionUID = 2421854498051377608L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                rulesModel.remove(rulesPanel.getSelection());
                                target.add(rulesPanel);
                            }
                        });
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        add(
                rulesPanel =
                        new GeoServerTablePanel<ShortAdminRule>(
                                "rulesPanel", rulesModel = new GeofenceAdminRulesModel(), true) {

                            private static final long serialVersionUID = -9041215145551707243L;

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<ShortAdminRule> itemModel,
                                    Property<ShortAdminRule> property) {
                                if (property == GeofenceAdminRulesModel.BUTTONS) {
                                    return new ButtonPanel(
                                            id, (ShortAdminRule) itemModel.getObject());
                                }
                                return null;
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                remove.setEnabled(rulesPanel.getSelection().size() > 0);
                                target.add(remove);
                            }
                        });
        rulesPanel.add(new WebTheme());
        rulesPanel.add(new DragSource(Operation.MOVE).drag("tr"));
        rulesPanel.add(
                new DropTarget(Operation.MOVE) {
                    private static final long serialVersionUID = -2153630274380471165L;

                    public void onDrop(
                            AjaxRequestTarget target, Transfer transfer, Location location) {
                        if (location == null
                                || !(location.getComponent().getDefaultModel().getObject()
                                        instanceof ShortAdminRule)) {
                            return;
                        }
                        ShortAdminRule movedRule = transfer.getData();
                        ShortAdminRule targetRule =
                                (ShortAdminRule)
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
                        doReturn(GeofenceServerAdminPage.class);
                    }
                }.dropCenter("tr"));
        rulesPanel.setOutputMarkupId(true);
    }

    private class ButtonPanel extends Panel {

        private static final long serialVersionUID = -3702358364804495550L;

        private ImageAjaxLink<Object> upLink;

        private ImageAjaxLink<Object> downLink;

        public ButtonPanel(String id, final ShortAdminRule rule) {
            super(id);
            this.setOutputMarkupId(true);

            upLink =
                    new ImageAjaxLink<Object>(
                            "up", new PackageResourceReference(getClass(), "img/arrow_up.png")) {
                        private static final long serialVersionUID = -8179503447106596760L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            rulesModel.moveUp(rule);
                            target.add(rulesPanel);
                        }

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (rulesModel.canUp(rule)) {
                                tag.put("style", "visibility:visible");
                            } else {
                                tag.put("style", "visibility:hidden");
                            }
                        }
                    };
            upLink.getImage()
                    .add(
                            new AttributeModifier(
                                    "alt",
                                    new ParamResourceModel("GeofenceServerAdminPage.up", upLink)));
            upLink.setOutputMarkupId(true);
            add(upLink);

            downLink =
                    new ImageAjaxLink<Object>(
                            "down",
                            new PackageResourceReference(getClass(), "img/arrow_down.png")) {
                        private static final long serialVersionUID = 4640187752303674221L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            rulesModel.moveDown(rule);
                            target.add(rulesPanel);
                        }

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (rulesModel.canDown(rule)) {
                                tag.put("style", "visibility:visible");
                            } else {
                                tag.put("style", "visibility:hidden");
                            }
                        }
                    };
            downLink.getImage()
                    .add(
                            new AttributeModifier(
                                    "alt",
                                    new ParamResourceModel(
                                            "GeofenceServerAdminPage.down", downLink)));
            downLink.setOutputMarkupId(true);
            add(downLink);

            ImageAjaxLink<Object> editLink =
                    new ImageAjaxLink<Object>(
                            "edit", new PackageResourceReference(getClass(), "img/edit.png")) {
                        private static final long serialVersionUID = 4640187752303674221L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            setResponsePage(new GeofenceAdminRulePage(rule, rulesModel));
                        }
                    };
            editLink.getImage()
                    .add(
                            new AttributeModifier(
                                    "alt",
                                    new ParamResourceModel(
                                            "GeofenceServerAdminPage.edit", editLink)));
            editLink.setOutputMarkupId(true);
            add(editLink);
        }
    }
}
