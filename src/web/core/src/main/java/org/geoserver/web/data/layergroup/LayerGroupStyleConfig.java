/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.wicket.LiveCollectionModel;

/**
 * Main LayerGroupStyles component. Holds the list of layer group styles and provides buttons to add
 * new or to copy an existing one.
 */
public class LayerGroupStyleConfig extends PublishedConfigurationPanel<LayerGroupInfo> {

    private ListView<LayerGroupStyle> styleListView;
    private WebMarkupContainer listContainer;
    private DropDownChoice<LayerGroupStyle> availableStyles;
    private IModel<LayerGroupInfo> layerGroupModel;

    LiveCollectionModel<LayerGroupStyle, List<LayerGroupStyle>> listModel;
    AjaxLink<LayerGroupStyle> copyLink;

    public LayerGroupStyleConfig(String id, IModel<LayerGroupInfo> model) {
        super(id, model);
        this.layerGroupModel = model;
        PropertyModel<List<LayerGroupStyle>> stylesModel =
                new PropertyModel<>(model, "layerGroupStyles");
        this.listModel = LiveCollectionModel.list(stylesModel);
        styleListView =
                new ListView<LayerGroupStyle>("styleList", listModel) {
                    @Override
                    protected void populateItem(ListItem<LayerGroupStyle> listItem) {
                        LayerGroupStyleModel lgStyle =
                                new LayerGroupStyleModel(listItem.getModel());
                        LayerGroupStylePanel stylePanel =
                                new LayerGroupStylePanel(
                                        "layerGroupStylePanel",
                                        lgStyle,
                                        new PropertyModel<>(model, "workspace")) {
                                    @Override
                                    protected void handleRemoval(
                                            AjaxRequestTarget target, LayerGroupStyle style) {
                                        List<LayerGroupStyle> styles =
                                                new ArrayList<>(listModel.getObject());
                                        styles.removeIf(s -> s.getId().equals(style.getId()));
                                        listModel.setObject(styles);
                                        listContainer.modelChanged();
                                        styleListView.modelChanged();
                                        availableStyles.setChoices(getAvailableStyles());
                                        target.add(listContainer, availableStyles);
                                        target.addChildren(styleListView, LayerGroupStyle.class);
                                    }
                                };
                        stylePanel.setOutputMarkupId(true);
                        listItem.add(stylePanel);
                        listItem.setOutputMarkupId(true);
                    }
                };
        styleListView.setOutputMarkupId(true);
        styleListView.setReuseItems(true);
        add(addNewLink());
        add(availableStyles = availableStyle(getAvailableStyles()));
        add(copyLink = copyLink());
        availableStyles.setOutputMarkupId(true);
        listContainer = new WebMarkupContainer("listContainer");
        listContainer.add(styleListView);
        listContainer.setOutputMarkupId(true);
        add(listContainer);
        LayerGroupInfo.Mode mode = model.getObject().getMode();
        setVisible(
                mode.equals(LayerGroupInfo.Mode.SINGLE)
                        || mode.equals(LayerGroupInfo.Mode.OPAQUE_CONTAINER));
    }

    private DropDownChoice<LayerGroupStyle> availableStyle(List<LayerGroupStyle> styles) {
        ChoiceRenderer<LayerGroupStyle> render =
                new ChoiceRenderer<LayerGroupStyle>() {
                    @Override
                    public Object getDisplayValue(LayerGroupStyle object) {
                        String displayVal;
                        if (object == null) displayVal = "default";
                        else displayVal = object.getName().getName();
                        return displayVal;
                    }
                };
        DropDownChoice<LayerGroupStyle> dropDownChoice =
                new DropDownChoice<>("availableStyles", new Model<>(), styles, render);
        dropDownChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        if (availableStyles.getInput() != null) {
                            LayerGroupStyle groupStyle = availableStyles.getModelObject();
                            LayerGroupStyle newGroupStyle = new LayerGroupStyleImpl();
                            if (groupStyle == null) {
                                LayerGroupInfo groupInfo = layerGroupModel.getObject();
                                populateStyle(
                                        newGroupStyle,
                                        groupInfo.getLayers(),
                                        groupInfo.getStyles());
                            } else {
                                populateStyle(
                                        newGroupStyle,
                                        groupStyle.getLayers(),
                                        groupStyle.getStyles());
                            }
                            copyLink.setModelObject(newGroupStyle);
                            target.add(availableStyles);
                        }
                    }
                });
        return dropDownChoice;
    }

    private AjaxLink<LayerGroupStyle> addNewLink() {
        return new AjaxLink<LayerGroupStyle>("addNew") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<LayerGroupStyle> groupStyles = styleListView.getModelObject();
                groupStyles = new ArrayList<>(groupStyles);
                groupStyles.add(new LayerGroupStyleImpl());
                styleListView.setModelObject(groupStyles);
                target.add(listContainer);
                target.addChildren(styleListView, LayerGroupStyle.class);
                target.addChildren(styleListView, LayerGroupStylePanel.class);
            }
        };
    }

    private AjaxLink<LayerGroupStyle> copyLink() {
        return new AjaxLink<LayerGroupStyle>("copy", new Model<>()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                LayerGroupStyle groupStyle = getModelObject();
                if (groupStyle != null) {
                    List<LayerGroupStyle> groupStyles = listModel.getObject();
                    LayerGroupStyle toAdd = new LayerGroupStyleImpl();
                    toAdd.setLayers(groupStyle.getLayers());
                    toAdd.setStyles(groupStyle.getStyles());
                    groupStyles = new ArrayList<>(groupStyles);
                    groupStyles.add(toAdd);
                    listModel.setObject(groupStyles);
                    target.add(listContainer);
                    target.addChildren(styleListView, LayerGroupStylePanel.class);
                } else {
                    availableStyles.warn("Please select a style");
                }
            }
        };
    }

    private void populateStyle(
            LayerGroupStyle lgStyle, List<PublishedInfo> layers, List<StyleInfo> styles) {
        for (int i = 0; i < layers.size(); i++) {
            lgStyle.getLayers().add(layers.get(i));
            lgStyle.getStyles().add(styles.get(i));
        }
    }

    private List<LayerGroupStyle> getAvailableStyles() {
        List<LayerGroupStyle> styles = listModel.getObject();
        styles =
                styles.stream()
                        .filter(s -> s != null && s.getName().getName() != null)
                        .collect(Collectors.toList());
        styles.add(0, null);
        return styles;
    }
}
