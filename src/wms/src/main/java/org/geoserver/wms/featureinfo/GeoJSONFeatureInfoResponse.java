/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a
 * GetFeatureInfo request.
 *
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
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
     * @see {@link GetFeatureInfoOutputFormat#write(FeatureCollectionType, GetFeatureInfoRequest,
     *     OutputStream)}
     */
    @Override
    public void write(
            FeatureCollectionType features, GetFeatureInfoRequest fInfoReq, OutputStream out)
            throws IOException {

        GeoJSONGetFeatureResponse format =
                new GeoJSONGetFeatureResponse(wms.getGeoServer(), getContentType());
        format.write(features, out, null);
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
