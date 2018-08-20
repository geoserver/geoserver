/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.UpDownPanel;

/** Allows to edit the list of layers contained in a layer group */
public class LayerGroupEntryPanel extends Panel {

    private static final long serialVersionUID = -5483938812185582866L;

    public static final Property<LayerGroupEntry> LAYER_TYPE =
            new PropertyPlaceholder<LayerGroupEntry>("layerType");

    public static final Property<LayerGroupEntry> LAYER =
            new PropertyPlaceholder<LayerGroupEntry>("layer");

    public static final Property<LayerGroupEntry> DEFAULT_STYLE =
            new PropertyPlaceholder<LayerGroupEntry>("defaultStyle");

    public static final Property<LayerGroupEntry> STYLE =
            new PropertyPlaceholder<LayerGroupEntry>("style");

    public static final Property<LayerGroupEntry> REMOVE =
            new PropertyPlaceholder<LayerGroupEntry>("remove");

    static final List<Property<LayerGroupEntry>> PROPERTIES =
            Arrays.asList(LAYER_TYPE, LAYER, DEFAULT_STYLE, STYLE, REMOVE);

    ModalWindow popupWindow;
    GeoServerTablePanel<LayerGroupEntry> layerTable;
    List<LayerGroupEntry> items;
    GeoServerDialog dialog;

    public LayerGroupEntryPanel(
            String id, LayerGroupInfo layerGroup, IModel<WorkspaceInfo> groupWorkspace) {
        super(id);

        items = new ArrayList<LayerGroupEntry>();
        for (int i = 0; i < layerGroup.getLayers().size(); i++) {
            PublishedInfo layer = layerGroup.getLayers().get(i);
            StyleInfo style = layerGroup.getStyles().get(i);
            items.add(new LayerGroupEntry(layer, style));
        }

        add(popupWindow = new ModalWindow("popup"));
        add(dialog = new GeoServerDialog("dialog"));
        add(new HelpLink("layersHelp").setDialog(dialog));
        add(new HelpLink("styleGroupHelp").setDialog(dialog));

        // make sure we don't end up serializing the list, but get it fresh from the dataProvider,
        // to avoid serialization issues seen in GEOS-8273
        LoadableDetachableModel<List<Property<LayerGroupEntry>>> propertiesModel =
                new LoadableDetachableModel<List<Property<LayerGroupEntry>>>() {
                    @Override
                    protected List<Property<LayerGroupEntry>> load() {
                        return PROPERTIES;
                    }
                };
        // layers
        add(
                layerTable =
                        new ReorderableTablePanel<LayerGroupEntry>(
                                "layers", items, propertiesModel) {

                            private static final long serialVersionUID = -3270471094618284639L;

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<LayerGroupEntry> itemModel,
                                    Property<LayerGroupEntry> property) {
                                if (property == LAYER_TYPE) {
                                    return typeLink(id, itemModel);
                                }
                                if (property == LAYER) {
                                    return layerLink(id, itemModel);
                                }
                                if (property == DEFAULT_STYLE) {
                                    return defaultStyleCheckbox(id, itemModel);
                                }
                                if (property == STYLE) {
                                    return styleLink(id, itemModel);
                                }
                                if (property == REMOVE) {
                                    return removeLink(id, itemModel);
                                }

                                return null;
                            }
                        }.setFilterable(false));
        layerTable.setItemReuseStrategy(new DefaultItemReuseStrategy());
        layerTable.setOutputMarkupId(true);
        layerTable.setPageable(false);

        add(
                new AjaxLink<LayerInfo>("addLayer") {
                    private static final long serialVersionUID = -6143440041597461787L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                        popupWindow.setContent(
                                new LayerListPanel(
                                        popupWindow.getContentId(), groupWorkspace.getObject()) {
                                    private static final long serialVersionUID =
                                            -47811496174289699L;

                                    @Override
                                    protected void handleLayer(
                                            LayerInfo layer, AjaxRequestTarget target) {
                                        popupWindow.close(target);

                                        items.add(
                                                new LayerGroupEntry(
                                                        layer, layer.getDefaultStyle()));

                                        // getCatalog().save( lg );
                                        target.add(layerTable);
                                    }
                                });

                        popupWindow.show(target);
                    }
                });

        add(
                new AjaxLink<LayerGroupInfo>("addLayerGroup") {
                    private static final long serialVersionUID = -6600366636542152188L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setTitle(new ParamResourceModel("chooseLayerGroup", this));
                        popupWindow.setContent(
                                new LayerGroupListPanel(
                                        popupWindow.getContentId(), groupWorkspace.getObject()) {
                                    private static final long serialVersionUID =
                                            4052338807144204692L;

                                    @Override
                                    protected void handleLayerGroup(
                                            LayerGroupInfo layerGroup, AjaxRequestTarget target) {
                                        popupWindow.close(target);

                                        items.add(new LayerGroupEntry(layerGroup, null));

                                        target.add(layerTable);
                                    }
                                });

                        popupWindow.show(target);
                    }
                });

        add(
                new AjaxLink<LayerGroupInfo>("addStyleGroup") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setTitle(new ParamResourceModel("chooseStyleGroup", this));
                        popupWindow.setContent(
                                new StyleListPanel(
                                        popupWindow.getContentId(),
                                        new StyleListPanel.StyleListProvider()) {

                                    @Override
                                    protected void handleStyle(
                                            StyleInfo style, AjaxRequestTarget target) {
                                        popupWindow.close(target);
                                        items.add(new LayerGroupEntry(null, style));
                                        target.add(layerTable);
                                    }
                                });

                        popupWindow.show(target);
                    }
                });
    }

    public List<LayerGroupEntry> getEntries() {
        return items;
    }

    Component typeLink(String id, IModel<LayerGroupEntry> itemModel) {
        LayerGroupEntry entry = itemModel.getObject();
        return new Label(id, "<i>" + entry.getType().toString() + "</i>")
                .setEscapeModelStrings(false);
    }

    Component layerLink(String id, IModel<LayerGroupEntry> itemModel) {
        LayerGroupEntry entry = itemModel.getObject();
        return new Label(id, entry.getLayer() == null ? "" : entry.getLayer().prefixedName());
    }

    Component defaultStyleCheckbox(String id, IModel<LayerGroupEntry> itemModel) {
        final LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        Fragment f = new Fragment(id, "defaultStyle", this);
        CheckBox ds = new CheckBox("checkbox", new Model<Boolean>(entry.isDefaultStyle()));
        ds.add(
                new OnChangeAjaxBehavior() {

                    private static final long serialVersionUID = 7700386104410665242L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean useDefault = (Boolean) getComponent().getDefaultModelObject();
                        entry.setDefaultStyle(useDefault);
                        target.add(layerTable);
                    }
                });
        f.add(ds);
        return f;
    }

    Component styleLink(String id, final IModel<LayerGroupEntry> itemModel) {
        // decide if the style is the default and the current style name
        LayerGroupEntry entry = itemModel.getObject();
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
                new SimpleAjaxLink<String>(id, new Model<String>(styleName)) {

                    private static final long serialVersionUID = 4677068931971673637L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setTitle(new ParamResourceModel("chooseStyle", this));
                        popupWindow.setContent(
                                new StyleListPanel(popupWindow.getContentId()) {
                                    private static final long serialVersionUID =
                                            -8463999379475701401L;

                                    @Override
                                    protected void handleStyle(
                                            StyleInfo style, AjaxRequestTarget target) {
                                        popupWindow.close(target);

                                        LayerGroupEntry entry =
                                                (LayerGroupEntry) itemModel.getObject();
                                        entry.setStyle(style);

                                        // redraw
                                        target.add(layerTable);
                                    }
                                });
                        popupWindow.show(target);
                    }
                };
        link.getLink().setEnabled(!defaultStyle);
        return link;
    }

    Component removeLink(String id, IModel<LayerGroupEntry> itemModel) {
        final LayerGroupEntry entry = itemModel.getObject();
        ImageAjaxLink<Object> link =
                new ImageAjaxLink<Object>(
                        id,
                        new PackageResourceReference(
                                getClass(), "../../img/icons/silk/delete.png")) {

                    private static final long serialVersionUID = 4050942811476326745L;

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
                                new ParamResourceModel("LayerGroupEditPage.th.remove", link)));
        return link;
    }

    Component positionPanel(String id, IModel<LayerGroupEntry> itemModel) {
        ParamResourceModel upTitle = new ParamResourceModel("moveToBottom", this);
        ParamResourceModel downTitle = new ParamResourceModel("moveToBottom", this);
        return new UpDownPanel<LayerGroupEntry>(
                id, itemModel.getObject(), items, layerTable, upTitle, downTitle);
    }
}
