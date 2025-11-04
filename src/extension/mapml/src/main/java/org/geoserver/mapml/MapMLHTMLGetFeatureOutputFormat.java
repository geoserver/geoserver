/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.mapml.tcrs.MapMLProjection;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;

/** Handles a GetFeature request that produces output in MapML HTML format. */
public class MapMLHTMLGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {
    private static final Logger LOGGER = Logging.getLogger(MapMLHTMLGetFeatureOutputFormat.class);

    /** @param gs the GeoServer instance */
    public MapMLHTMLGetFeatureOutputFormat(GeoServer gs) {
        super(
                gs,
                new LinkedHashSet<>(
                        Arrays.asList(MapMLConstants.MAPML_HTML_MIME_TYPE, MapMLConstants.HTML_FORMAT_NAME)));
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MapMLConstants.MAPML_HTML_MIME_TYPE;
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollectionResponse, OutputStream out, Operation getFeature)
            throws IOException, ServiceException {
        Request request = Dispatcher.REQUEST.get();
        HttpServletRequest httpRequest = request.getHttpRequest();

        List<FeatureCollection> featureCollections = featureCollectionResponse.getFeatures();
        if (featureCollections.size() != 1) {
            throw new ServiceException("MapML OutputFormat does not support Multiple Feature Type output.");
        }
        FeatureCollection featureCollection = featureCollections.get(0);
        if (!(featureCollection instanceof SimpleFeatureCollection)) {
            throw new ServiceException("MapML OutputFormat does not support Complex Features.");
        }

        // What if there is a reprojection?
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;
        ReferencedEnvelope projectedBbox = extractBbox(fc);
        LayerInfo layerInfo = gs.getCatalog().getLayerByName(fc.getSchema().getTypeName());
        CoordinateReferenceSystem crs = projectedBbox.getCoordinateReferenceSystem();
        MapMLProjection projType = parseProjType(request);
        double longitude;
        double latitude;
        ReferencedEnvelope geographicBox;
        try {
            geographicBox = projectedBbox.transform(DefaultGeographicCRS.WGS84, true);
            longitude = geographicBox.centre().getX();
            latitude = geographicBox.centre().getY();
        } catch (TransformException | FactoryException e) {
            throw new ServiceException("Unable to transform bbox to WGS84", e);
        }

        boolean flipCoords = CRS.getAxisOrder(crs).equals(CRS.AxisOrder.NORTH_EAST);
        double lat = flipCoords ? longitude : latitude;
        double lon = flipCoords ? latitude : longitude;

        MapMLHTMLOutput output = new MapMLHTMLOutput.HTMLOutputBuilder()
                .setLongitude(lon)
                .setLatitude(lat)
                .setRequest(httpRequest)
                .setLayerLabel(layerInfo.getTitle())
                .setProjType(projType)
                .setProjectedBbox(projectedBbox)
                .setSourceUrL(buildGetFeature(request))
                .build();

        // write to output
        OutputStreamWriter osw = new OutputStreamWriter(out, gs.getSettings().getCharset());
        osw.write(output.toHTML());
        osw.flush();
    }

    private MapMLProjection parseProjType(Request request) throws ServiceException {
        try {
            Map<String, Object> rawKvp = request.getRawKvp();
            String srs;
            if (rawKvp.containsKey("SRSNAME")) {
                srs = (String) rawKvp.get("SRSNAME");
            } else if (rawKvp.containsKey("SRS")) {
                srs = (String) rawKvp.get("SRS");
            } else {
                srs = "EPSG:4326";
            }
            return new MapMLProjection(srs.toUpperCase());
        } catch (IllegalArgumentException | FactoryException iae) {
            // figure out the parameter name (version dependent) and the actual original
            // string value for the srs/crs parameter
            String parameterName = Optional.ofNullable(request.getVersion())
                    .filter(v -> v.equals("1.0.0"))
                    .map(v -> "srs")
                    .orElse("srsName");
            Map<String, Object> rawKvp = Dispatcher.REQUEST.get().getRawKvp();
            String value = (String) rawKvp.get("SRSNAME");
            if (value == null) value = (String) rawKvp.get("SRS");
            throw new ServiceException(
                    "This projection is not supported by MapML: " + value,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    parameterName);
        }
    }

    private ReferencedEnvelope extractBbox(SimpleFeatureCollection fc) {
        ReferencedEnvelope bbox = null;
        if (fc != null && !fc.isEmpty()) {
            bbox = fc.getBounds();
        }
        if (bbox == null) {
            if (fc instanceof TypeInfoCollectionWrapper wrapper) {
                try {
                    bbox = wrapper.getFeatureTypeInfo().boundingBox();
                } catch (Exception e) {
                    LOGGER.warning(
                            "Unable to retrieve the bounding box of the underlying feature collection. Returning whole world");
                    bbox = new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
                }
            }
        }
        return bbox;
    }

    private String buildGetFeature(Request request) {
        Map<String, Object> rawKvp = request.getRawKvp();
        String baseUrl = ResponseUtils.baseURL(request.getHttpRequest());
        Map<String, String> kvp = extractKvp(rawKvp);

        return ResponseUtils.buildURL(baseUrl, "wfs", kvp, URLMangler.URLType.SERVICE);
    }

    private LinkedHashMap<String, String> extractKvp(Map<String, Object> rawKvp) {
        LinkedHashMap<String, String> kvp = rawKvp.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> "OUTPUTFORMAT".equals(entry.getKey())
                                ? MapMLConstants.FORMAT_NAME
                                : entry.getValue().toString(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        kvp.putIfAbsent("OUTPUTFORMAT", MapMLConstants.FORMAT_NAME);
        return kvp;
    }

    @Override
    public List<String> getCapabilitiesElementNames() {
        return Collections.singletonList(MapMLConstants.HTML_FORMAT_NAME);
    }
}
