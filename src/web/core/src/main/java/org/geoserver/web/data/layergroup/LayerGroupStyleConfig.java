/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.wicket.LiveCollectionModel;

/**
 * Main LayerGroupStyles component. Holds the list of layer group styles and provides buttons to add
 * new or to copy an existing one.
 */
public class LayerGroupStyleConfig extends PublishedConfigurationPanel<LayerGroupInfo> {

    private ListView<LayerGroupStyle> styleListView;
    private WebMarkupContainer listContainer;
    private LiveCollectionModel<LayerGroupStyle, List<LayerGroupStyle>> listModel;
    private DropDownChoice<LayerGroupStyle> availableStyles;

    @SuppressWarnings("unused")
    private LayerGroupStyle copySelection;

    public LayerGroupStyleConfig(String id, IModel<LayerGroupInfo> model) {
        super(id, model);
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
                                        availableStyles.setChoices(styles);
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
        add(availableStyles = availableStyle(stylesModel.getObject()));
        add(copyLink());
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
                        return object.getName().getName();
                    }
                };
        PropertyModel<LayerGroupStyle> model = new PropertyModel<>(this, "copySelection");
        DropDownChoice<LayerGroupStyle> dropDownChoice =
                new DropDownChoice<LayerGroupStyle>("availableStyles", model, styles, render) {

                    @Override
                    protected void onSelectionChanged(LayerGroupStyle newSelection) {
                        super.onSelectionChanged(newSelection);
                        this.setModelObject(newSelection);
                    }

                    @Override
                    protected boolean wantOnSelectionChangedNotifications() {
                        return true;
                    }
                };
        return dropDownChoice;
    }

    private AjaxLink<LayerGroupStyle> addNewLink() {
        return new AjaxLink<LayerGroupStyle>("addNew") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<LayerGroupStyle> groupStyles = styleListView.getModelObject();
                groupStyles = new ArrayList<>(groupStyles);
                groupStyles.add(new LayerGroupStyle());
                styleListView.setModelObject(groupStyles);
                target.add(listContainer);
                target.addChildren(styleListView, LayerGroupStyle.class);
                target.addChildren(styleListView, LayerGroupStylePanel.class);
            }
        };
    }

    private AjaxLink<LayerGroupStyle> copyLink() {
        return new AjaxLink<LayerGroupStyle>("copy") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                LayerGroupStyle groupStyle = availableStyles.getModelObject();
                if (groupStyle == null) {
                    warn("Please select a layer group style");
                    return;
                }
                List<LayerGroupStyle> groupStyles = listModel.getObject();
                groupStyles = new ArrayList<>(groupStyles);
                LayerGroupStyle newGroupStyle = new LayerGroupStyle();
                newGroupStyle.setLayers(groupStyle.getLayers());
                newGroupStyle.setStyles(groupStyle.getStyles());
                groupStyles.add(newGroupStyle);
                styleListView.setModelObject(groupStyles);
                availableStyles.setModel(new Model<>());
                target.add(listContainer, availableStyles);
                target.addChildren(styleListView, LayerGroupStyle.class);
                target.addChildren(styleListView, LayerGroupStylePanel.class);
            }
        };
    }
}
