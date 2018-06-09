/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layergroup.LayerGroupEntryPanel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.wms.eo.EoLayerType;

/** Allows to edit the list of layers contained in a layer group */
@SuppressWarnings("serial")
public class EoLayerGroupEntryPanel extends Panel {

    ModalWindow popupWindow;
    LayerGroupEntryProvider entryProvider;
    GeoServerTablePanel<EoLayerGroupEntry> layerTable;
    List<EoLayerGroupEntry> items;
    EoLayerType layerType;
    String layerGroupName;

    public EoLayerGroupEntryPanel(String id, LayerGroupInfo layerGroup, ModalWindow popupWindow) {
        super(id);
        this.popupWindow = popupWindow;

        Catalog catalog = GeoServerApplication.get().getCatalog();

        this.layerGroupName = layerGroup.getName();
        items = new ArrayList<EoLayerGroupEntry>();
        for (int i = 0; i < layerGroup.getLayers().size(); i++) {
            PublishedInfo layer = layerGroup.getLayers().get(i);
            StyleInfo style = layerGroup.getStyles().get(i);
            if (style == null) {
                LayerInfo li = catalog.getLayer(layer.getId());
                if (layer != null) {
                    style = li.getDefaultStyle();
                }
            }
            items.add(new EoLayerGroupEntry((LayerInfo) layer, style, layerGroup.getName()));
        }

        // layers
        final EoLayerTypeRenderer eoLayerTypeRenderer = new EoLayerTypeRenderer();
        entryProvider = new LayerGroupEntryProvider(items);
        layerTable =
                new GeoServerTablePanel<EoLayerGroupEntry>("layers", entryProvider) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<EoLayerGroupEntry> itemModel,
                            Property<EoLayerGroupEntry> property) {
                        if (property == LayerGroupEntryProvider.LAYER) {
                            EoLayerGroupEntry entry = (EoLayerGroupEntry) itemModel.getObject();
                            return new Label(id, entry.getLayer().prefixedName());
                        }
                        if (property == LayerGroupEntryProvider.TYPE) {
                            EoLayerType type =
                                    (EoLayerType) property.getModel(itemModel).getObject();
                            return new Label(
                                    id, (String) eoLayerTypeRenderer.getDisplayValue(type));
                        }
                        if (property == LayerGroupEntryProvider.STYLE) {
                            return styleLink(id, itemModel);
                        }
                        if (property == LayerGroupEntryProvider.REMOVE) {
                            return removeLink(id, itemModel);
                        }
                        if (property == LayerGroupEntryProvider.POSITION) {
                            return positionPanel(id, itemModel);
                        }

                        return null;
                    }
                };
        layerTable.setFilterable(false);
        layerTable.setSortable(false);
        layerTable.setOutputMarkupId(true);
        layerTable.setItemReuseStrategy(new DefaultItemReuseStrategy());
        add(layerTable);
    }

    public List<EoLayerGroupEntry> getEntries() {
        return items;
    }

    Component styleLink(String id, final IModel itemModel) {
        // decide if the style is the default and the current style name
        EoLayerGroupEntry entry = (EoLayerGroupEntry) itemModel.getObject();
        String styleName = null;
        boolean defaultStyle = true;
        if (entry.getStyle() != null) {
            styleName = entry.getStyle().getName();
            defaultStyle = false;
        } else if (entry.getLayer() instanceof LayerInfo) {
            LayerInfo layer = (LayerInfo) entry.getLayer();
            if (layer.getDefaultStyle() != null) {
                styleName = layer.getDefaultStyle().getName();
            }
        }

        // build and returns the link, but disable it if the style is the default
        SimpleAjaxLink<String> link =
                new SimpleAjaxLink<String>(id, new Model(styleName)) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final EoLayerGroupEntry entry = (EoLayerGroupEntry) itemModel.getObject();

                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setTitle(new ParamResourceModel("chooseStyle", this));
                        popupWindow.setContent(
                                new EoStyleListPanel(
                                        popupWindow.getContentId(), entry.getLayerType()) {
                                    @Override
                                    protected void handleStyle(
                                            StyleInfo style, AjaxRequestTarget target) {
                                        entry.setStyle(style);

                                        // redraw
                                        target.add(layerTable);
                                        popupWindow.close(target);
                                    }
                                });
                        popupWindow.show(target);
                    }
                };
        link.getLink().setEnabled(!defaultStyle);
        return link;
    }

    Component removeLink(String id, IModel itemModel) {
        final EoLayerGroupEntry entry = (EoLayerGroupEntry) itemModel.getObject();
        ImageAjaxLink link =
                new ImageAjaxLink(
                        id,
                        new PackageResourceReference(
                                LayerGroupEntryPanel.class, "../../img/icons/silk/delete.png")) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {

                        items.remove(entry);
                        target.add(layerTable);
                    }
                };
        link.getImage()
                .add(
                        new AttributeModifier(
                                "alt",
                                new ParamResourceModel("AbstractLayerGroupPage.th.remove", link)));
        return link;
    }

    Component positionPanel(String id, IModel itemModel) {
        return new PositionPanel(id, (EoLayerGroupEntry) itemModel.getObject());
    }

    static class LayerGroupEntryProvider extends GeoServerDataProvider<EoLayerGroupEntry> {

        public static Property<EoLayerGroupEntry> LAYER =
                new PropertyPlaceholder<EoLayerGroupEntry>("sourceLayer");

        public static Property<EoLayerGroupEntry> LAYER_SUBNAME =
                new BeanProperty<EoLayerGroupEntry>("layer", "layerSubName");

        public static Property<EoLayerGroupEntry> STYLE =
                new PropertyPlaceholder<EoLayerGroupEntry>("style");

        public static Property<EoLayerGroupEntry> TYPE =
                new BeanProperty<EoLayerGroupEntry>("layerType", "layerType");

        public static Property<EoLayerGroupEntry> REMOVE =
                new PropertyPlaceholder<EoLayerGroupEntry>("remove");

        public static Property<EoLayerGroupEntry> POSITION =
                new PropertyPlaceholder<EoLayerGroupEntry>("position");

        static List PROPERTIES = Arrays.asList(POSITION, LAYER, LAYER_SUBNAME, TYPE, STYLE, REMOVE);

        List<EoLayerGroupEntry> items;

        public LayerGroupEntryProvider(List<EoLayerGroupEntry> items) {
            this.items = items;
        }

        @Override
        protected List<EoLayerGroupEntry> getItems() {
            return items;
        }

        @Override
        protected List<Property<EoLayerGroupEntry>> getProperties() {
            return PROPERTIES;
        }
    }

    class PositionPanel extends Panel {

        EoLayerGroupEntry entry;
        private ImageAjaxLink upLink;
        private ImageAjaxLink downLink;

        public PositionPanel(String id, final EoLayerGroupEntry entry) {
            super(id);
            this.entry = entry;
            this.setOutputMarkupId(true);

            upLink =
                    new ImageAjaxLink(
                            "up",
                            new PackageResourceReference(
                                    LayerGroupEntryPanel.class,
                                    "../../img/icons/silk/arrow_up.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            int index = items.indexOf(PositionPanel.this.entry);
                            items.remove(index);
                            items.add(Math.max(0, index - 1), PositionPanel.this.entry);
                            target.add(layerTable);
                            target.add(this);
                            target.add(downLink);
                            target.add(upLink);
                        }

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (items.indexOf(entry) == 0) {
                                tag.put("style", "visibility:hidden");
                            } else {
                                tag.put("style", "visibility:visible");
                            }
                        }
                    };
            upLink.getImage()
                    .add(new AttributeModifier("alt", new ParamResourceModel("up", upLink)));
            upLink.setOutputMarkupId(true);
            add(upLink);

            downLink =
                    new ImageAjaxLink(
                            "down",
                            new PackageResourceReference(
                                    LayerGroupEntryPanel.class,
                                    "../../img/icons/silk/arrow_down.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            int index = items.indexOf(PositionPanel.this.entry);
                            items.remove(index);
                            items.add(Math.min(items.size(), index + 1), PositionPanel.this.entry);
                            target.add(layerTable);
                            target.add(this);
                            target.add(downLink);
                            target.add(upLink);
                        }

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (items.indexOf(entry) == items.size() - 1) {
                                tag.put("style", "visibility:hidden");
                            } else {
                                tag.put("style", "visibility:visible");
                            }
                        }
                    };
            downLink.getImage()
                    .add(new AttributeModifier("alt", new ParamResourceModel("down", downLink)));
            downLink.setOutputMarkupId(true);
            add(downLink);
        }
    }

    public void setLayerGroupName(String layerGroupName) {
        this.layerGroupName = layerGroupName;
    }
}
