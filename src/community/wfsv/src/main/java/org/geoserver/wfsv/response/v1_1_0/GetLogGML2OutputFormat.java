/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.wfsv.response.v1_1_0;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfsv.GetLogType;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.xml.GML2OutputFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Extends GML2OutputFormat to allow GetLog output to be encoded in GML2
 * @author Andrea Aime
 * @author David Winslow
 *
 */
public class GetLogGML2OutputFormat extends GML2OutputFormat {

    public GetLogGML2OutputFormat( GeoServer geoserver) {
        super(geoserver);
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
            Operation getFeature) throws IOException, ServiceException {

        GetLogType request = (GetLogType) getFeature.getParameters()[0];
        GetFeatureRequest ftRequest = toGetFeatureType(featureCollection, request);

        prepare(ftRequest.getOutputFormat(), featureCollection, ftRequest);
        encode(output, featureCollection, ftRequest);
    }

    /**
     * Turns a GetLogType objects into an almost equivalent GetFeatureType object s
     * that the superclass can do its work
     * @param featureCollection
     * @param request
     * @return
     */
    private GetFeatureRequest toGetFeatureType(FeatureCollectionResponse featureCollection,
            GetLogType request) {
        SimpleFeatureCollection features = (SimpleFeatureCollection) featureCollection.getFeature().get(0);
        SimpleFeatureType featureType = features.getSchema();
        GetFeatureType ftRequest = WfsFactory.eINSTANCE.createGetFeatureType();
        QueryType query = WfsFactory.eINSTANCE.createQueryType();
        query.setTypeName(Collections.singletonList(featureType.getTypeName()));
        ftRequest.getQuery().add(query);
        ftRequest.setBaseUrl(request.getBaseUrl());
        ftRequest.setHandle(request.getHandle());
        ftRequest.setMaxFeatures(request.getMaxFeatures());
        ftRequest.setOutputFormat(request.getOutputFormat());
        ftRequest.setResultType(ResultTypeType.RESULTS_LITERAL);
        return GetFeatureRequest.adapt(ftRequest);
    }

    public boolean canHandle(Operation operation) {
        // GetFeature operation?
        if ("GetLog".equalsIgnoreCase(operation.getId())) {
            // also check that the resultType is "results"
            GetLogType request = (GetLogType) OwsUtils.parameter(operation.getParameters(),
                    GetLogType.class);

            return request.getResultType() == ResultTypeType.RESULTS_LITERAL;
        }

        return false;
    }

}
