/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapCallback;
import org.geoserver.wms.GetMapCallbackAdapter;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.MergeBehavior;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.RasterLayer;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;

/**
 * This {@link GetMapCallback} is responsible for enforcing that an EO GetMap works as per the spec:
 *
 * <p>-1- 1 or 3 Bands requested -2- Set the merge behavior for the underlying mosaic
 *
 * @author Simone Giannecchini, GeoSOlutions SAS
 */
public class EOGetMapChecker extends GetMapCallbackAdapter implements GetMapCallback {
    /** BAND_COVERAGE_VALUE */
    private static final String BAND_COVERAGE_VALUE = EoLayerType.BAND_COVERAGE.name();

    Catalog catalog;

    public EOGetMapChecker(Catalog catalog) {
        this.catalog = catalog;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public WMSMapContent beforeRender(WMSMapContent content) {

        // is this WMS 1.3.0? If not move along
        final GetMapRequest request = content.getRequest();
        if (!"1.3.0".equalsIgnoreCase(request.getVersion())) {
            return super.beforeRender(content);
        }

        // === look for BANDS layer
        final List<Layer> layers = content.layers();
        RasterLayer layer = null;
        for (Layer tempLayer : layers) {
            if (tempLayer instanceof RasterLayer) {
                String title = tempLayer.getTitle();
                LayerInfo layerInfo = catalog.getLayerByName(title);
                if (layerInfo != null) {
                    MetadataMap metadata = layerInfo.getMetadata();
                    if (metadata.containsKey(EoLayerType.KEY)
                            && metadata.get(EoLayerType.KEY).equals(BAND_COVERAGE_VALUE)) {
                        layer = (RasterLayer) tempLayer;
                        break;
                    }
                }
            }
        }
        if (layer == null) {
            // there is no BAND layer, we move on as usual
            return super.beforeRender(content);
        }

        // get the underlying grid reader
        final GridReaderLayer readerLayer = (GridReaderLayer) layer;
        final GeneralParameterValue[] params = readerLayer.getParams();

        // get the read parameters for this reader
        try {
            final Set<ParameterDescriptor<List>> dynamicParameters =
                    readerLayer.getReader().getDynamicParameters();
            if (dynamicParameters.isEmpty()) {
                throw new IllegalStateException(
                        "Layer "
                                + readerLayer.getTitle()
                                + " has no additional dimensions which are required for an EO BANDS layer");
            }
            final Set<ReferenceIdentifier> dynamicParametersNames =
                    new HashSet<ReferenceIdentifier>();
            for (ParameterDescriptor<List> param : dynamicParameters) {
                dynamicParametersNames.add(param.getName());
            }

            // looking for the readparams to control dimensions
            // -1- control band stacking
            // -2- check that a valid number of bands has been called
            boolean foundMergeBehavior = false;
            int foundCustomDimensions = 0;
            for (int i = 0; i < params.length; i++) {
                final ParameterValue param = (ParameterValue) params[i];
                final ParameterDescriptor descriptor = param.getDescriptor();
                final ReferenceIdentifier name = descriptor.getName();

                // MERGE_BEHAVIOR
                if (name.equals(ImageMosaicFormat.MERGE_BEHAVIOR.getName())) {
                    foundMergeBehavior = true;
                    param.setValue(MergeBehavior.STACK.toString());
                } else {
                    // Dynamic Parameters checks
                    // -1- only one can have multiple values with cardinality 3
                    if (dynamicParametersNames.contains(name)) {
                        final List paramValues = (List) param.getValue();
                        final int size = paramValues.size();
                        if (size != 1 && size != 3) {
                            throw new ServiceException(
                                    "Wrong number of values provided to this GetMap for EO BANDS layer. Paremeter:"
                                            + name.getCode()
                                            + " #:"
                                            + size,
                                    "InvalidDimensionValue");
                        }
                        foundCustomDimensions++;
                    }
                }
            }
            // did we find all the custom dimensions
            if (foundCustomDimensions != dynamicParameters.size()) {
                throw new IllegalArgumentException(
                        "Not all the dimensions for this EO BANDS layer were requested. Please, check the GetMap request.");
            }

            // check if we found and set the merge behavior
            if (!foundMergeBehavior) {
                // should not happen
                throw new IllegalStateException("Unable to impose Stacking merge behavior!");
            }

            // move on as usual
            return super.beforeRender(content);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }
}
