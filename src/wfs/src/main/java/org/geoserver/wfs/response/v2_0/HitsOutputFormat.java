/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.v2_0;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.ResultTypeType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xsd.Encoder;

public class HitsOutputFormat extends org.geoserver.wfs.response.HitsOutputFormat {

    public HitsOutputFormat(GeoServer gs) {
        super(gs, new WFSConfiguration());
    }

    @Override
    public boolean canHandle(Operation operation) {
        GetFeatureType request =
                OwsUtils.parameter(operation.getParameters(), GetFeatureType.class);
        return request != null && request.getResultType() == ResultTypeType.HITS;
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {
        String format = getGetFeatureType(operation).getOutputFormat();
        if (isJSONResponse(format)) {
            return format;
        } else return "text/xml";
    }

    @Override
    protected void encode(
            FeatureCollectionResponse hits, OutputStream output, WFSInfo wfs, Operation operation)
            throws IOException {
        GetFeatureType getFeatureType = getGetFeatureType(operation);
        // if output format is JSON, use the GeoJSON response via composition
        if (isJSONResponse(getFeatureType.getOutputFormat())) {
            GeoJSONGetFeatureResponse geoJSONResponse =
                    new GeoJSONGetFeatureResponse(gs, getFeatureType.getOutputFormat());
            geoJSONResponse.write(hits, output, operation);
        } else {
            // no JSON, encode GML
            encodeGML(hits, output, wfs);
        }
    }

    private boolean isJSONResponse(String format) {
        return "application/geo+json".equals(format)
                || "application/json".equals(format)
                || "text/javascript".equals(format);
    }

    private void encodeGML(FeatureCollectionResponse hits, OutputStream output, WFSInfo wfs)
            throws IOException {
        hits.setNumberOfFeatures(BigInteger.valueOf(0));
        Encoder e = new Encoder(new WFSConfiguration());
        e.setEncoding(Charset.forName(wfs.getGeoServer().getSettings().getCharset()));
        e.setSchemaLocation(
                WFS.NAMESPACE, ResponseUtils.appendPath(wfs.getSchemaBaseURL(), "wfs/2.0/wfs.xsd"));

        e.encode(hits.getAdaptee(), WFS.FeatureCollection, output);
    }

    private GetFeatureType getGetFeatureType(Operation operation) {
        GetFeatureType ftype = OwsUtils.parameter(operation.getParameters(), GetFeatureType.class);
        return ftype;
    }
}
