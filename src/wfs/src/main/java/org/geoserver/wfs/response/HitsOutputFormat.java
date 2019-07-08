/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.ResultTypeType;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;

/**
 * WFS output format for a GetFeature operation in which the resultType is "hits".
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class HitsOutputFormat extends WFSResponse {

    /** Xml configuration */
    Configuration configuration;

    public HitsOutputFormat(GeoServer gs, Configuration configuration) {
        super(gs, FeatureCollectionResponse.class);

        this.configuration = configuration;
    }

    /** @return "text/xml"; */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    /** Checks that the resultType is of type "hits". */
    public boolean canHandle(Operation operation) {
        GetFeatureType request =
                (GetFeatureType)
                        OwsUtils.parameter(operation.getParameters(), GetFeatureType.class);

        return (request != null) && (request.getResultType() == ResultTypeType.HITS_LITERAL);
    }

    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        WFSInfo wfs = getInfo();

        FeatureCollectionResponse featureCollection = (FeatureCollectionResponse) value;

        // create a new feautre collcetion type with just the numbers
        FeatureCollectionResponse hits = featureCollection.create();
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

        hits.setTotalNumberOfFeatures(featureCollection.getTotalNumberOfFeatures());
        hits.setNext(featureCollection.getNext());
        hits.setPrevious(featureCollection.getPrevious());
        hits.setTimeStamp(featureCollection.getTimeStamp());

        encode(hits, output, wfs);
    }

    private BigInteger countFeature(FeatureCollectionResponse fct) {
        BigInteger count = BigInteger.valueOf(0);
        for (int fcIndex = 0; fcIndex < fct.getFeature().size(); fcIndex++) {
            FeatureIterator i = null;
            try {
                for (i = (((FeatureCollection) fct.getFeature().get(fcIndex)).features());
                        i.hasNext();
                        i.next()) {
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

    protected void encode(FeatureCollectionResponse hits, OutputStream output, WFSInfo wfs)
            throws IOException {
        XSDSchema result;
        try {
            result = configuration.getXSD().getSchema();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Encoder encoder = new Encoder(configuration, result);
        encoder.setEncoding(Charset.forName(wfs.getGeoServer().getSettings().getCharset()));
        encoder.setSchemaLocation(
                org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                ResponseUtils.appendPath(wfs.getSchemaBaseURL(), "wfs/1.1.0/wfs.xsd"));

        encoder.encode(
                hits.getAdaptee(), org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION, output);
    }
}
