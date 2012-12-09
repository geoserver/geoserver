/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.xml.namespace.QName;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wms.GetFeatureInfo;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.opengis.feature.type.Name;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a GetFeatureInfo request.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 * 
 */
public class GeoJSONFeatureInfoResponse extends GetFeatureInfoOutputFormat {

    protected final WMS wms;

    /**
     * @param wms
     * @param outputFormat
     * @throws Exception if outputFormat is not a valid json mime type
     */
    public GeoJSONFeatureInfoResponse(final WMS wms, final String outputFormat) throws Exception {
        super(outputFormat);
        this.wms = wms;
    }

    /**
     * Writes a Json (or Jsonp) response on the passed output stream
     * 
     * @see {@link GetFeatureInfoOutputFormat#write(FeatureCollectionType, GetFeatureInfoRequest, OutputStream)}
     */

    /**
     * @param value {@link FeatureCollectionType}
     * @param output where to encode the results to
     * @param operation {@link GetFeatureInfo}
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream, org.geoserver.platform.Operation)
     */
    @Override
    public void write(FeatureCollectionType features, GetFeatureInfoRequest fInfoReq,
            OutputStream out) throws IOException {

        // the 'request' object we'll pass to our OutputFormat
        GetFeatureType gfreq = WfsFactory.eINSTANCE.createGetFeatureType();
        gfreq.setBaseUrl(fInfoReq.getBaseUrl());

        final int size = features.getFeature().size();
        for (int i = 0; i < size; i++) {

            FeatureCollection fc = (FeatureCollection) features.getFeature().get(i);
            Name name = FeatureCollectionDecorator.getName(fc);
            QName qname = new QName(name.getNamespaceURI(), name.getLocalPart());

            features.getFeature().add(fc);

            QueryType qt = WfsFactory.eINSTANCE.createQueryType();

            qt.setTypeName(Collections.singletonList(qname));

            String crs = GML2EncodingUtils.epsgCode(fc.getSchema().getCoordinateReferenceSystem());
            if (crs != null) {
                final String srsName = "EPSG:" + crs;
                try {
                    qt.setSrsName(new URI(srsName));

                } catch (URISyntaxException e) {
                    throw new IOException("Unable to determite coordinate system for featureType "
                            + fc.getSchema().getName().getLocalPart() + ".  Schema told us '"
                            + srsName + "'", e);
                }
            }
            gfreq.getQuery().add(qt);

        }

        // this is a dummy wrapper around our 'request' object so that the new Dispatcher will
        // accept it.
        Service serviceDesc = new Service("wms", null, null, Collections.EMPTY_LIST);
        Operation opDescriptor = new Operation("", serviceDesc, null, new Object[] { gfreq });

        final GeoServer gs = wms.getGeoServer();
        final String contentType = getContentType();
        GeoJSONGetFeatureResponse format = new GeoJSONGetFeatureResponse(gs, contentType);
        format.write(features, out, opDescriptor);
    }

}
