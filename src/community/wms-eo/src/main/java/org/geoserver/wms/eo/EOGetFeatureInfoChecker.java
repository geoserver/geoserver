/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.util.NullProgressListener;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.MergeBehavior;
import org.geotools.util.factory.GeoTools;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.ReferenceIdentifier;

/**
 * {@link EOGetFeatureInfoChecker} is supposed to manipulate the GetFeatureInfo request in order to
 * comply with WMS-EO Requirements.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class EOGetFeatureInfoChecker extends AbstractDispatcherCallback
        implements DispatcherCallback {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        // paranoiac checks
        if (operation == null || operation.getParameters().length <= 0) {
            return super.operationDispatched(request, operation);
        }

        // === get the underlying operation and check if this is a feature info or not
        final Object o = operation.getParameters()[0];
        if (!(o instanceof GetFeatureInfoRequest)) {
            return super.operationDispatched(request, operation);
        }

        // === k, this is a getfeatureinfo request let's check if it is 1.3.0
        org.geoserver.wms.GetFeatureInfoRequest featureinfoReq = (GetFeatureInfoRequest) o;
        if (!featureinfoReq.getVersion().equalsIgnoreCase("1.3.0")) {
            return super.operationDispatched(request, operation);
        }

        // === ok, it is a getfeatureinfo and 1.3.0, let's do the magic!

        // inspect the incoming request for custom dimensions
        final Map<String, String> rawKvpMap = request.getRawKvp();
        Map<String, List> customDomains = new HashMap<String, List>();
        if (rawKvpMap != null) {
            for (Map.Entry<String, String> kvp : rawKvpMap.entrySet()) {
                String name = kvp.getKey();
                if (name.startsWith("DIM_")) {
                    name = name.substring(4);
                    if (name.length() > 0 && name != null) {
                        final ArrayList<String> val = new ArrayList<String>(1);
                        if (kvp.getValue().indexOf(",") > 0) {
                            String[] elements = kvp.getValue().split(",");
                            val.addAll(Arrays.asList(elements));
                        } else {
                            val.add(kvp.getValue());
                        }
                        customDomains.put(name, val);
                    }
                }
            }
        }

        // cycle on all the requested layers and make sure we check the custom dimensions
        final List<MapLayerInfo> queryLayers = featureinfoReq.getQueryLayers();
        for (MapLayerInfo queryLayerInfo : queryLayers) {

            // geoserver info objects
            final CoverageInfo cInfo = queryLayerInfo.getCoverage();
            final LayerInfo layerInfo = queryLayerInfo.getLayerInfo();

            // is it a BANDS Layer?
            final MetadataMap metadata = layerInfo.getMetadata();
            if (metadata.containsKey(EoLayerType.KEY)
                    && metadata.get(EoLayerType.KEY).equals(EoLayerType.BAND_COVERAGE.name())) {

                // check the MERGE_BEHAVIOR as flat (this is harmless anyway)
                Map<String, Serializable> params = cInfo.getParameters();
                for (Entry<String, Serializable> entry : params.entrySet()) {
                    if (entry.getKey()
                            .equalsIgnoreCase(
                                    ImageMosaicFormat.MERGE_BEHAVIOR.getName().getCode())) {
                        entry.setValue(MergeBehavior.STACK.toString());
                        break;
                    }
                }
                try {
                    // check the #of requested values
                    // get the read parameters for this reader
                    final GridCoverageReader gridCoverageReader =
                            cInfo.getGridCoverageReader(
                                    new NullProgressListener(), GeoTools.getDefaultHints());
                    final Set<ParameterDescriptor<List>> dynamicParameters =
                            ((GridCoverage2DReader) gridCoverageReader).getDynamicParameters();
                    if (dynamicParameters.isEmpty()) {
                        throw new IllegalStateException(
                                "Layer "
                                        + cInfo.getTitle()
                                        + " has no additional dimensions which are required for an EO BANDS layer");
                    }
                    final Set<ReferenceIdentifier> dynamicParametersNames =
                            new HashSet<ReferenceIdentifier>();
                    for (ParameterDescriptor<List> param : dynamicParameters) {
                        dynamicParametersNames.add(param.getName());
                    }

                    // inspect the incoming request
                    for (ParameterDescriptor<List> readParam : dynamicParameters) {
                        final String name = readParam.getName().getCode();

                        // ok, do we have this one in the request? If so, do we have 1 or 3 values?
                        if (customDomains.containsKey(name)) {
                            final List values = customDomains.get(name);
                            // 1 or 3, 0 to leave default kick in
                            if (values.size() != 0 && values.size() != 1 && values.size() != 3) {
                                throw new ServiceException(
                                        "Dimension DIM_"
                                                + name
                                                + " has been request with wrong number of values: "
                                                + values.size(),
                                        "InvalidDimensionValue");
                            }

                            // NOTICE that implicitly we leave the default do its magic if no val
                        }
                    }

                } catch (IOException e) {
                    throw new IllegalStateException(
                            "Unable to acquire a reader for CoverageInfo: " + cInfo, e);
                }
            }
        }
        return super.operationDispatched(request, operation);
    }
}
