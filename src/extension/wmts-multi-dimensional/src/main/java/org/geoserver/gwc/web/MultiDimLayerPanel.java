/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.MultiDimensionalExtension;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.util.logging.Logging;

/**
 * Configures expansion limits on a layer by layer basis. TODO: would be nice if this panel could go
 * into the Dimension tab, but there are no extension points there.
 */
public class MultiDimLayerPanel extends PublishedConfigurationPanel<LayerInfo> {

    static final Logger LOGGER = Logging.getLogger(MultiDimLayerPanel.class);

    public MultiDimLayerPanel(String id, IModel<? extends LayerInfo> layerModel) {
        super(id, layerModel);

        WebMarkupContainer sidecarTypeContainer = new WebMarkupContainer("sidecarTypeContainer");
        add(sidecarTypeContainer);

        PropertyModel<MetadataMap> metadataModel =
                new PropertyModel<>(layerModel, "resource.metadata");
        MapModel<String> sidecarTypeModel =
                new MapModel<>(metadataModel, MultiDimensionalExtension.SIDECAR_TYPE);
        DropDownChoice<String> sidecarType =
                new DropDownChoice<>("sidecarType", sidecarTypeModel, getAvailableTypes());
        sidecarType.add(new SidecarValidator());
        sidecarTypeContainer.add(sidecarType);
        LayerInfo layer = (LayerInfo) getDefaultModelObject();
        sidecarTypeContainer.setVisible(layer.getResource() instanceof FeatureTypeInfo);

        MapModel<Integer> expandLimitDefaultModel =
                new MapModel<>(metadataModel, MultiDimensionalExtension.EXPAND_LIMIT_KEY);
        TextField<Integer> expandLimitDefault =
                new TextField<>("defaultExpandLimit", expandLimitDefaultModel, Integer.class);
        expandLimitDefault.add(RangeValidator.minimum(0));
        add(expandLimitDefault);

        MapModel<Integer> expandLimitMaxModel =
                new MapModel<>(metadataModel, MultiDimensionalExtension.EXPAND_LIMIT_MAX_KEY);
        TextField<Integer> expandLimitMax =
                new TextField<>("maxExpandLimit", expandLimitMaxModel, Integer.class);
        expandLimitMax.add(RangeValidator.minimum(0));
        add(expandLimitMax);
    }

    private List<String> getAvailableTypes() {
        try {
            LayerInfo layer = (LayerInfo) getDefaultModelObject();
            ResourceInfo resource = layer.getResource();
            if (!(resource instanceof FeatureTypeInfo)) return Collections.emptyList();

            DataAccess<? extends FeatureType, ? extends Feature> store =
                    ((DataStoreInfo) resource.getStore()).getDataStore(null);
            return store.getNames().stream()
                    .map(n -> n.getLocalPart())
                    .filter(t -> t != null && !t.equals(layer.getResource().getNativeName()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            error(getErrorMessage("sidecarTypesLoadError"));
            LOGGER.log(Level.SEVERE, "Failed to load list of feature types", e);
            return Collections.emptyList();
        }
    }

    private class SidecarValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> validatable) {
            try {
                LayerInfo layer = (LayerInfo) getDefaultModelObject();
                String sidecarType = validatable.getValue();
                String nativeType = layer.getResource().getNativeName();

                DataStore store =
                        (DataStore)
                                ((DataStoreInfo) layer.getResource().getStore()).getDataStore(null);
                Map<String, AttributeDescriptor> sidecarAttributes =
                        getAttributesMap(store, sidecarType);
                Map<String, AttributeDescriptor> mainAttributes =
                        getAttributesMap(store, nativeType);

                layer.getResource()
                        .getMetadata()
                        .forEach(
                                (k, v) -> {
                                    // validate enabled dimensions
                                    if (v instanceof DimensionInfo) {
                                        DimensionInfo di = (DimensionInfo) v;
                                        if (di.isEnabled()) {
                                            validateDimension(
                                                    di,
                                                    sidecarType,
                                                    sidecarAttributes,
                                                    mainAttributes);
                                        }
                                    }
                                });

            } catch (IOException e) {
                LOGGER.log(
                        Level.SEVERE, "Failed to load the target feature type for validation", e);
            }
        }

        private void validateDimension(
                DimensionInfo di,
                String sidecarTypeName,
                Map<String, AttributeDescriptor> sidecarAttributes,
                Map<String, AttributeDescriptor> mainAttributes) {
            validateAttribute(
                    di.getAttribute(), sidecarTypeName, sidecarAttributes, mainAttributes);
            if (di.getEndAttribute() != null) {
                validateAttribute(
                        di.getEndAttribute(), sidecarTypeName, sidecarAttributes, mainAttributes);
            }
        }

        private void validateAttribute(
                String attribute,
                String sidecarTypeName,
                Map<String, AttributeDescriptor> sidecarAttributes,
                Map<String, AttributeDescriptor> mainAttributes) {
            if (!sidecarAttributes.containsKey(attribute)
                    || !mainAttributes.containsKey(attribute)) {
                error(getErrorMessage("attributeNotFound", attribute, sidecarTypeName));
                return;
            }
            Class<?> expectedType = mainAttributes.get(attribute).getType().getBinding();
            if (!sidecarAttributes.get(attribute).getType().getBinding().equals(expectedType)) {
                error(
                        getErrorMessage(
                                "attributeTypeMismatch",
                                attribute,
                                sidecarTypeName,
                                expectedType.getSimpleName()));
            }
        }

        private Map<String, AttributeDescriptor> getAttributesMap(
                DataStore store, String sidecarType) throws IOException {
            SimpleFeatureType sidecarSchema = store.getSchema(sidecarType);
            return sidecarSchema.getAttributeDescriptors().stream()
                    .collect(Collectors.toMap(ad -> ad.getLocalName(), ad -> ad));
        }
    }

    private String getErrorMessage(String attributeNotFound, Object... resources) {
        return new ParamResourceModel(attributeNotFound, this, resources).getString();
    }
}
