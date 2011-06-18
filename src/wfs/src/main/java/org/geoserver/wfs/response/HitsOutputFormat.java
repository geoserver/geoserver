/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Iterator;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.xml.Encoder;


/**
 * WFS output format for a GetFeature operation in which the resultType is "hits".
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class HitsOutputFormat extends WFSResponse {
    
    /**
     * Xml configuration
     */
    WFSConfiguration configuration;

    public HitsOutputFormat(GeoServer gs, WFSConfiguration configuration) {
        super(gs, FeatureCollectionType.class);

        this.configuration = configuration;
    }

    /**
     * @return "text/xml";
     */
    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return "text/xml";
    }

    /**
     * Checks that the resultType is of type "hits".
     */
    public boolean canHandle(Operation operation) {
        GetFeatureType request = (GetFeatureType) OwsUtils.parameter(operation.getParameters(),
                GetFeatureType.class);

        return (request != null) && (request.getResultType() == ResultTypeType.HITS_LITERAL);
    }

    public void write(Object value, OutputStream output, Operation operation)
        throws IOException, ServiceException {
        WFSInfo wfs = getInfo();
        
        FeatureCollectionType featureCollection = (FeatureCollectionType) value;

        //create a new feautre collcetion type with just the numbers
        FeatureCollectionType hits = WfsFactory.eINSTANCE.createFeatureCollectionType();
        if (GML3OutputFormat.isComplexFeature(featureCollection)) {
            // we have to count the number of features here manually because complex feature
            // collection size() now returns 0. In order to count the number of features,
            // we have to build the features to count them and this has great performance
            // impact. Unless we introduce joins in our fetching of
            // data, we will have to count the number of features manually when needed. In
            // GML3Outputformat I use xslt to populate numberOfFeatures attribute.
            hits.setNumberOfFeatures(countFeature(featureCollection));
        } else {
            hits.setNumberOfFeatures(featureCollection.getNumberOfFeatures());
        }
        hits.setTimeStamp(featureCollection.getTimeStamp());

        Encoder encoder = new Encoder(configuration, configuration.schema());
        encoder.setEncoding(Charset.forName( wfs.getGeoServer().getGlobal().getCharset()) );
        encoder.setSchemaLocation(org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
            ResponseUtils.appendPath(wfs.getSchemaBaseURL(), "wfs/1.1.0/wfs.xsd"));

        encoder.encode(hits, org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION, output);
    }
    
    private BigInteger countFeature(FeatureCollectionType fct) {
        BigInteger count = BigInteger.valueOf(0);
        for (int fcIndex = 0; fcIndex < fct.getFeature().size(); fcIndex++) {
            FeatureIterator i = null;
            try {
                for (i = (((FeatureCollection) fct.getFeature().get(fcIndex)).features()); i
                        .hasNext(); i.next()) {
                    count = count.add(BigInteger.ONE);
                }
            } finally {
                if (i != null) {
                    i.close();
                }
            }
        }
        return count;
    }

   

}