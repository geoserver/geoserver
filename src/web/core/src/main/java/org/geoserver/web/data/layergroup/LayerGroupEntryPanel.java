/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.web.wicket.GSModalWindow;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** Allows to edit the list of layers contained in a layer group */
public abstract class LayerGroupEntryPanel<T> extends Panel {

    @Serial
    private static final long serialVersionUID = -5483938812185582866L;

    public static final Property<LayerGroupEntry> LAYER_TYPE = new PropertyPlaceholder<>("layerType");

    public static final Property<LayerGroupEntry> LAYER = new PropertyPlaceholder<>("layer");

    public static final Property<LayerGroupEntry> DEFAULT_STYLE = new PropertyPlaceholder<>("defaultStyle");

    public static final Property<LayerGroupEntry> STYLE = new PropertyPlaceholder<>("style");

    public static final Property<LayerGroupEntry> REMOVE = new PropertyPlaceholder<>("remove");

    static final List<Property<LayerGroupEntry>> PROPERTIES =
            Arrays.asList(LAYER_TYPE, LAYER, DEFAULT_STYLE, STYLE, REMOVE);

    GSModalWindow popupWindow;
    GeoServerTablePanel<LayerGroupEntry> layerTable;
    List<LayerGroupEntry> items;
    GeoServerDialog dialog;

    /**
     * @param id the panel id.
     * @param tModel the Model.
     * @param groupWorkspace the LayerGroup workspace to filter published info that can be added.
     */
    public LayerGroupEntryPanel(String id, IModel<T> tModel, IModel<WorkspaceInfo> groupWorkspace) {
        super(id);
        initUI(getLayers(tModel.getObject()), getStyles(tModel.getObject()), groupWorkspace);
    }

    /**
     * @param id the panel id.
     * @param tModel the model.
     * @param groupWorkspace the LayerGroup workspace to filter published info that can be added.
     * @param horizontalBtn flag to require the add entry button to be displayed horizontally instead of vertically.
     * @param bigLegendTitle flag to require the panel title to be render as a big fieldset label (true) or as a normal
     *     field label (false).
     */
    public LayerGroupEntryPanel(
            String id,
            IModel<T> tModel,
            IModel<WorkspaceInfo> groupWorkspace,
            boolean horizontalBtn,
            boolean bigLegendTitle) {
        super(id);
        initUI(getLayers(tModel.getObject()), getStyles(tModel.getObject()), groupWorkspace, bigLegendTitle);
    }

    /**
     * @param id the panel id.
     * @param t the container object.
     * @param groupWorkspace the LayerGroup workspace to filter published info that can be added.
     */
    public LayerGroupEntryPanel(String id, T t, IModel<WorkspaceInfo> groupWorkspace) {
        super(id);
        initUI(getLayers(t), getStyles(t), groupWorkspace);
    }

    /**
     * @param id the panel id.
     * @param layers the list of PublishedInfo to be added as entries.
     * @param styles the list of StyleInfo to be added as entries.
     * @param groupWorkspace the LayerGroup workspace to filter published info that can be added.
     */
    public LayerGroupEntryPanel(
            String id, List<PublishedInfo> layers, List<StyleInfo> styles, IModel<WorkspaceInfo> groupWorkspace) {
        super(id);
        initUI(layers, styles, groupWorkspace);
    }

    private void initUI(List<PublishedInfo> layers, List<StyleInfo> styles, IModel<WorkspaceInfo> groupWorkspace) {
        initUI(layers, styles, groupWorkspace, true);
    }

    private void initUI(
            List<PublishedInfo> layers,
            List<StyleInfo> styles,
            IModel<WorkspaceInfo> groupWorkspace,
            boolean bigLegendTitle) {
        items = new ArrayList<>();
        for (int i = 0; i < layers.size(); i++) {
            PublishedInfo layer = layers.get(i);
            StyleInfo style = styles.get(i);
            items.add(new LayerGroupEntry(layer, style));
        }

        add(popupWindow = new GSModalWindow("popup"));
        add(dialog = new GeoServerDialog("dialog"));
        add(panelTitle(bigLegendTitle));
        // make sure we don't end up serializing the list, but get it fresh from the dataProvider,
        // to avoid serialization issues seen in GEOS-8273
        LoadableDetachableModel<List<Property<LayerGroupEntry>>> propertiesModel = new LoadableDetachableModel<>() {
            @Override
            protected List<Property<LayerGroupEntry>> load() {
                return PROPERTIES;
            }
        };
        // layers
        add(
                layerTable = new ReorderableTablePanel<>("layers", LayerGroupEntry.class, items, propertiesModel) {

                    @Serial
                    private static final long serialVersionUID = -3270471094618284639L;

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<LayerGroupEntry> itemModel, Property<LayerGroupEntry> property) {
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
        add(addLayer(groupWorkspace));
        add(addLayerGroup(groupWorkspace));
        add(addStyleGroup());
        add(styleGroupHelp());
    }

    private Fragment panelTitle(boolean legendTitle) {
        String id = legendTitle ? "bigLegend" : "smallLegend";
        Fragment fragment = new Fragment("panelTitle", id, this);
        if (legendTitle) fragment.add(new HelpLink("layersHelp").setDialog(dialog));
        return fragment;
    }

    private AjaxLink<LayerInfo> addLayer(IModel<WorkspaceInfo> groupWorkspace) {
        return new AjaxLink<>("addLayer") {
            @Serial
            private static final long serialVersionUID = -6143440041597461787L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight(375);
                popupWindow.setInitialWidth(525);
                popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                popupWindow.setContent(new LayerListPanel(popupWindow.getContentId(), groupWorkspace.getObject()) {
                    @Serial
                    private static final long serialVersionUID = -47811496174289699L;

                    @Override
                    protected void handleLayer(LayerInfo layer, AjaxRequestTarget target) {
                        popupWindow.close(target);

                        items.add(new LayerGroupEntry(layer, layer.getDefaultStyle()));

                        // getCatalog().save( lg );
                        target.add(layerTable);
                    }
                });

                popupWindow.show(target);
            }
        };
    }

    private AjaxLink<LayerGroupInfo> addLayerGroup(IModel<WorkspaceInfo> groupWorkspace) {
        return new AjaxLink<>("addLayerGroup") {
            @Serial
            private static final long serialVersionUID = -6600366636542152188L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight(375);
                popupWindow.setInitialWidth(525);
                popupWindow.setTitle(new ParamResourceModel("chooseLayerGroup", this));
                popupWindow.setContent(new LayerGroupListPanel(popupWindow.getContentId(), groupWorkspace.getObject()) {
                    @Serial
                    private static final long serialVersionUID = 4052338807144204692L;

                    @Override
                    protected void handleLayerGroup(LayerGroupInfo layerGroup, AjaxRequestTarget target) {
                        popupWindow.close(target);

                        items.add(new LayerGroupEntry(layerGroup, null));

                        target.add(layerTable);
                    }
                });

                popupWindow.show(target);
            }
        };
    }

    private AjaxLink<LayerGroupInfo> addStyleGroup() {

        return new AjaxLink<>("addStyleGroup") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight(375);
                popupWindow.setInitialWidth(525);
                popupWindow.setTitle(new ParamResourceModel("chooseStyleGroup", this));
                popupWindow.setContent(
                        new StyleListPanel(popupWindow.getContentId(), new StyleListPanel.StyleListProvider()) {

                            @Override
                            protected void handleStyle(StyleInfo style, AjaxRequestTarget target) {
                                popupWindow.close(target);
                                items.add(new LayerGroupEntry(null, style));
                                target.add(layerTable);
                            }
                        });

                popupWindow.show(target);
            }
        };
    }

    private HelpLink styleGroupHelp() {
        return new HelpLink("styleGroupHelp").setDialog(dialog);
    }

    public List<LayerGroupEntry> getEntries() {
        return items;
    }

    Component typeLink(String id, IModel<LayerGroupEntry> itemModel) {
        LayerGroupEntry entry = itemModel.getObject();
        return new Label(id, "<i>" + entry.getType().toString() + "</i>").setEscapeModelStrings(false);
    }

    Component layerLink(String id, IModel<LayerGroupEntry> itemModel) {
        LayerGroupEntry entry = itemModel.getObject();
        return new Label(id, entry.getLayer() == null ? "" : entry.getLayer().prefixedName());
    }

    Component defaultStyleCheckbox(String id, IModel<LayerGroupEntry> itemModel) {
        final LayerGroupEntry entry = itemModel.getObject();
        Fragment f = new Fragment(id, "defaultStyle", this);
        CheckBox ds = new CheckBox("checkbox", new Model<>(entry.isDefaultStyle()));
        ds.add(new OnChangeAjaxBehavior() {

            @Serial
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
        } else if (entry.getLayer() instanceof LayerGroupInfo) {
            LayerGroupInfo group = (LayerGroupInfo) entry.getLayer();
            List<LayerGroupStyle> groupStyles = group.getLayerGroupStyles();
            if (groupStyles != null && !groupStyles.isEmpty()) defaultStyle = false;
        }

        // build and returns the link, but disable it if the style is the default
        SimpleAjaxLink<String> link = new SimpleAjaxLink<>(id, new Model<>(styleName)) {

            @Serial
            private static final long serialVersionUID = 4677068931971673637L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight(375);
                popupWindow.setInitialWidth(525);
                popupWindow.setTitle(new ParamResourceModel("chooseStyle", this));
                popupWindow.setContent(
                        new StyleListPanel(
                                popupWindow.getContentId(),
                                itemModel.getObject().getLayer()) {
                            @Serial
                            private static final long serialVersionUID = -8463999379475701401L;

                            @Override
                            protected void handleStyle(StyleInfo style, AjaxRequestTarget target) {
                                popupWindow.close(target);

                                LayerGroupEntry entry = itemModel.getObject();
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
                new ImageAjaxLink<>(id, new PackageResourceReference(getClass(), "../../img/icons/silk/delete.png")) {

                    @Serial
                    private static final long serialVersionUID = 4050942811476326745L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {

                        items.remove(entry);
                        target.add(layerTable);
                    }
                };
        link.getImage().add(new AttributeModifier("alt", new ParamResourceModel("LayerGroupEditPage.th.remove", link)));
        return link;
    }

    /**
     * Get the PublishedInfo List to be added to the entries list.
     *
     * @param object the container from which retrieve the PublishedInfo list.
     * @return the List of PublishedInfo contained by the object container.
     */
    protected abstract List<PublishedInfo> getLayers(T object);

    /**
     * Get the StyleInfo List to be added to the entries list.
     *
     * @param object the container from which retrieve the StyleInfo list.
     * @return the List of StyleInfo contained by the object container.
     */
    protected abstract List<StyleInfo> getStyles(T object);
}
