/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.util.FeatureTypeDimensionsAccessor;
import org.geoserver.web.data.resource.VectorCustomDimensionEditor.RemoveEvent;
import org.geoserver.web.util.MetadataMapModel;

/**
 * Panel Component for editing custom dimensions on vector.
 *
 * @author Fernando Mino - Geosolutions
 */
public class VectorCustomDimensionsPanel extends Panel {

    private final IModel<FeatureTypeInfo> typeInfoModel;
    private final IModel<MetadataMap> metadata;
    private final WebMarkupContainer mainDiv;

    public VectorCustomDimensionsPanel(String id, IModel<FeatureTypeInfo> typeInfoModel) {
        super(id, typeInfoModel);
        this.typeInfoModel = typeInfoModel;
        this.metadata = new PropertyModel<MetadataMap>(typeInfoModel, "metadata");

        this.setOutputMarkupId(true);

        mainDiv = new WebMarkupContainer("mainDiv");
        mainDiv.setOutputMarkupId(true);
        add(mainDiv);

        final Form addForm = new Form("form");
        addForm.setOutputMarkupId(true);
        mainDiv.add(addForm);

        // the sub container for adding new dimension input and button
        final WebMarkupContainer addDimContainer =
                new WebMarkupContainer("addVectorCustomDimensionPanel");
        addDimContainer.setOutputMarkupId(true);
        addForm.add(addDimContainer);

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        addDimContainer.add(feedback);
        // add the name input
        final TextField<String> nameInput =
                new TextField<String>("nameInput", new Model<String>(""));
        addDimContainer.add(nameInput);
        // add the ajax button
        final AjaxButton addButton =
                new AjaxButton("addButton", addForm) {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        nameInput.processInput();
                        final String newName = nameInput.getModelObject();
                        nameInput.getModel().setObject("");
                        // add new item with the provided name.
                        addNewDimension(newName);
                        // update the entire panel
                        target.add(mainDiv);
                    }
                };
        addButton.setDefaultFormProcessing(false);
        addDimContainer.add(addButton);

        // the sub container for dimensions refreshing view
        final RefreshingView<VectorCustomDimensionEntry> refreshingView =
                buildVectorCustomDimensionsView(typeInfoModel);
        refreshingView.setOutputMarkupId(true);
        addForm.add(refreshingView);
    }

    private void addNewDimension(String name) {
        if (StringUtils.isBlank(name) || !validateNewCustomDimensionName(name)) return;
        name = foldDimentionMapKey(name);
        final MetadataMap metadataMap = getMetadata().getObject();
        if (!metadataMap.containsKey(name)) {
            // add the new DimensionInfoImpl object
            DimensionInfo dimInfo = new DimensionInfoImpl();
            dimInfo.setEnabled(true);
            metadataMap.put(name, dimInfo);
        }
    }

    private boolean validateNewCustomDimensionName(String name) {
        name = name.toLowerCase().trim();
        if ("time".equals(name) || "elevation".equals(name)) {
            error("'" + name + "' not allowed for custom dimension name.");
            return false;
        }
        final String dimName = foldDimentionMapKey(name);
        final MetadataMap metadataMap = getMetadata().getObject();
        if (metadataMap.containsKey(dimName)) {
            error("'" + name + "' custom dimension already exists.");
            return false;
        }
        return true;
    }

    private String foldDimentionMapKey(String name) {
        return "dim_" + name.toLowerCase();
    }

    public IModel<FeatureTypeInfo> getTypeInfoModel() {
        return typeInfoModel;
    }

    public IModel<MetadataMap> getMetadata() {
        return metadata;
    }

    /** Builds the List needed for the refreshing view. */
    private List<IModel<VectorCustomDimensionEntry>> getCustomDimensionMetadataList(
            final IModel<FeatureTypeInfo> typeInfoModel) {
        final PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(getTypeInfoModel(), "metadata");
        final List<VectorDimensionModel> models = new ArrayList<>();
        final FeatureTypeInfo typeInfo = typeInfoModel.getObject();
        final FeatureTypeDimensionsAccessor accessor = new FeatureTypeDimensionsAccessor(typeInfo);
        final Map<String, DimensionInfo> customDimensions = accessor.getCustomDimensions();
        for (final Entry<String, DimensionInfo> dimension : customDimensions.entrySet()) {
            final String dimensionName = dimension.getKey();
            models.add(new VectorDimensionModel(metadata, dimensionName, DimensionInfo.class));
        }
        return (List) models;
    }

    private RefreshingView<VectorCustomDimensionEntry> buildVectorCustomDimensionsView(
            final IModel<FeatureTypeInfo> typeInfoModel) {
        final RefreshingView<VectorCustomDimensionEntry> view =
                new RefreshingView<VectorCustomDimensionEntry>("vectorCustomDimensionsView") {

                    @Override
                    protected Iterator<IModel<VectorCustomDimensionEntry>> getItemModels() {
                        return (Iterator) getCustomDimensionMetadataList(typeInfoModel).iterator();
                    }

                    @Override
                    protected void populateItem(Item<VectorCustomDimensionEntry> item) {
                        final VectorCustomDimensionEntry entry = item.getModel().getObject();
                        final WebMarkupContainer div =
                                new WebMarkupContainer("vectorCustomDimension");
                        div.setOutputMarkupId(true);
                        item.add(div);
                        div.add(new Label("dimensionName", entry.getKeyNoPrefixed()));
                        final VectorCustomDimensionEditor editor =
                                new VectorCustomDimensionEditor(
                                        "customVectorEditor",
                                        item.getModel(),
                                        typeInfoModel.getObject(),
                                        Serializable.class);
                        editor.disablePresentationMode(DimensionPresentation.CONTINUOUS_INTERVAL);
                        div.add(editor);
                    }
                };
        view.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        return view;
    }

    @Override
    public void onEvent(IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof RemoveEvent) {
            final RemoveEvent removeEvent = (RemoveEvent) event.getPayload();
            removeDimensionByKey(removeEvent.getEntry().getFormerKey());
            removeEvent.getTarget().add(mainDiv);
        }
    }

    private void removeDimensionByKey(String key) {
        this.metadata.getObject().remove(key);
    }

    /** Model for handling custom dimension renaming events on MetadataMap. */
    public static class VectorDimensionModel extends MetadataMapModel<VectorCustomDimensionEntry> {

        private static final String VECTOR_CUSTOM_DIMENSION_PREFIX = "dim_";

        boolean hasRange;
        boolean hasResolution;

        private String key;
        private VectorCustomDimensionEntry entry;

        public VectorDimensionModel(IModel<MetadataMap> model, String expression, Class<?> target) {
            super(model, expression, target);
            key = VECTOR_CUSTOM_DIMENSION_PREFIX + expression;
        }

        public VectorCustomDimensionEntry getObject() {
            final DimensionInfo dim =
                    (DimensionInfo)
                            model.getObject()
                                    .get(VECTOR_CUSTOM_DIMENSION_PREFIX + expression, target);
            if (entry == null) {
                final Pair<String, DimensionInfo> pair = Pair.of(key, dim);
                entry = new VectorCustomDimensionEntry(pair);
            } else {
                entry.setDimensionInfo(dim);
            }
            return entry;
        }

        public void setObject(VectorCustomDimensionEntry object) {
            // check if is a renamed dimension
            if (object.hasModifiedKey()) {
                // remove former key
                model.getObject().remove(object.getFormerKey());
            }
            model.getObject().put(object.getKey(), object.getDimensionInfo());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            VectorDimensionModel other = (VectorDimensionModel) obj;
            if (key == null) {
                if (other.key != null) return false;
            } else if (!key.equals(other.key)) return false;
            return true;
        }
    }
}
