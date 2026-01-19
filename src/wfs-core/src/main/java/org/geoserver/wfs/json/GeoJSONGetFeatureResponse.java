/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONException;
import org.geoserver.config.GeoServer;
import org.geoserver.json.GeoJSONFeatureWriter;
import org.geoserver.json.JSONType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.response.ComplexFeatureAwareFormat;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.FeatureCollection;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a GetFeatureInfo request.
 *
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 * @author Carsten Klein, DataGis
 */
public class GeoJSONGetFeatureResponse extends WFSGetFeatureOutputFormat implements ComplexFeatureAwareFormat {
    private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    // store the response type
    protected final boolean jsonp;

    public GeoJSONGetFeatureResponse(GeoServer gs, String format) {
        super(gs, format);
        this.jsonp = JSONType.isJsonpMimeType(format);
    }

    /**
     * Constructor to be used by subclasses.
     *
     * @param format The well-known name of the format, not {@code null}
     * @param jsonp {@code true} if specified format uses JSONP
     */
    protected GeoJSONGetFeatureResponse(GeoServer gs, String format, boolean jsonp) {
        super(gs, format);
        this.jsonp = jsonp;
    }

    /**
     * Returns the GeoJSON Feature writer used to generate the output. Subclasses can override it to use a different
     * {@link GeoJSONFeatureWriter subclass}
     */
    protected <T extends FeatureType, F extends Feature> GeoJSONFeatureWriter<T, F> getFeatureWriter(
            FeatureCollectionResponse response, Operation operation) {
        return new WFSGeoJSONFeatureWriter<>(gs, getMimeType(response, operation), response);
    }

    /** capabilities output format string. */
    @Override
    public String getCapabilitiesElementName() {
        return JSONType.getJSONType(
                        getOutputFormats().isEmpty()
                                ? null
                                : getOutputFormats().iterator().next())
                .toString();
    }

    /** Returns the mime type */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (jsonp) {
            return JSONType.JSONP.getMimeType();
        } else {
            return JSONType.JSON.getMimeType();
        }
    }

    @Override
    protected void write(FeatureCollectionResponse response, OutputStream output, Operation operation)
            throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("about to encode JSON");

        // get feature count for request
        BigInteger totalNumberOfFeatures = response.getTotalNumberOfFeatures();
        BigInteger featureCount =
                (totalNumberOfFeatures != null && totalNumberOfFeatures.longValue() < 0) ? null : totalNumberOfFeatures;

        try {
            @SuppressWarnings("unchecked")
            List<FeatureCollection<FeatureType, Feature>> collections = (List) response.getFeature();
            getFeatureWriter(response, operation).write(collections, output, featureCount, jsonp);
        } catch (JSONException jsonException) {
            throw new ServiceException("Error: " + jsonException.getMessage(), jsonException);
        }
    }

    /** Container class for information related with a group of features. */
    protected static class FeaturesInfo {

        final CoordinateReferenceSystem crs;
        final boolean hasGeometry;
        public long featureCount;

        protected FeaturesInfo(CoordinateReferenceSystem crs, boolean hasGeometry, long featureCount) {
            this.crs = crs;
            this.hasGeometry = hasGeometry;
            this.featureCount = featureCount;
        }
    }

    @Override
    public String getCharset(Operation operation) {
        return gs.getGlobal().getSettings().getCharset();
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return "json";
    }
}
