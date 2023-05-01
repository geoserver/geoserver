/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.data.resource.TitleAndAbstractPanel;

/** UI Component for a single LayerGroupStyle. */
public abstract class LayerGroupStylePanel extends FormComponentPanel<LayerGroupStyle> {

    private LayerGroupEntryPanel<LayerGroupStyle> groupEntryPanel;
    private TextField<String> name;

    public LayerGroupStylePanel(
            String id, LayerGroupStyleModel model, IModel<WorkspaceInfo> workspaceInfo) {
        super(id, model);
        IModel<StyleInfo> styleNameModel = new PropertyModel<>(model, "name");
        if (styleNameModel.getObject() == null)
            styleNameModel.setObject(
                    new StyleInfoImpl((Catalog) GeoServerExtensions.bean("catalog")));
        name = new TextField<>("layerGroupStyleName", model.nameModel());
        name.setRequired(true);
        add(name);
        TitleAndAbstractPanel titleAndAbstractPanel =
                new TitleAndAbstractPanel("titleAndAbstract", model, "titleMsg", "abstract", this);
        add(titleAndAbstractPanel);
        add(removeLink());
        this.groupEntryPanel =
                new LayerGroupEntryPanel<LayerGroupStyle>(
                        "layerGroupEntryPanel", model, workspaceInfo, true, false) {
                    @Override
                    protected List<PublishedInfo> getLayers(LayerGroupStyle object) {
                        return object.getLayers();
                    }

                    @Override
                    protected List<StyleInfo> getStyles(LayerGroupStyle object) {
                        return object.getStyles();
                    }
                };
        groupEntryPanel.setOutputMarkupId(true);
        add(groupEntryPanel);
    }

    private AjaxLink<LayerGroupStyle> removeLink() {
        return new AjaxLink<LayerGroupStyle>("remove") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                handleRemoval(target, LayerGroupStylePanel.this.getModelObject());
                groupEntryPanel.modelChanged();
                target.add(groupEntryPanel);
            }
        };
    }

    /**
     * Handle the removal of a LayerGroupStyle from the list. Subclasses needs to implement it.
     *
     * @param target the wicket AjaxRequestTarget to which add the component.
     * @param style the LayerGroupStyle to remove.
     */
    protected abstract void handleRemoval(AjaxRequestTarget target, LayerGroupStyle style);

    @Override
    public void convertInput() {
        updateStyleModel();
    }

    @Override
    public void updateModel() {
        updateStyleModel();
    }

    private void updateStyleModel() {
        List<LayerGroupEntry> layerGroupEntries = groupEntryPanel.getEntries();
        int size = layerGroupEntries.size();
        List<PublishedInfo> publishedInfos = new ArrayList<>(size);
        List<StyleInfo> styleInfos = new ArrayList<>(size);
        for (int i = 0; i < layerGroupEntries.size(); i++) {
            LayerGroupEntry entry = layerGroupEntries.get(i);
            if (publishedInfos.size() <= i) publishedInfos.add(i, entry.getLayer());
            else publishedInfos.set(i, entry.getLayer());
            if (styleInfos.size() <= i) styleInfos.add(i, entry.getStyle());
            else styleInfos.set(i, entry.getStyle());
        }
        LayerGroupStyleModel styleModel = getStyleModel();
        styleModel.setLayers(publishedInfos);
        styleModel.setStyles(styleInfos);
    }

    private LayerGroupStyleModel getStyleModel() {
        return (LayerGroupStyleModel) getModel();
    }
}
