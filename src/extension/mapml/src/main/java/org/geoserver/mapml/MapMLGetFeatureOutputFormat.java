/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import net.opengis.wfs.impl.GetFeatureTypeImpl;
import net.opengis.wfs.impl.QueryTypeImpl;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * @author Chris Hodgson
 * @author prushforth
 */
public class MapMLGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {
    private static final Logger LOGGER = Logging.getLogger(MapMLGetFeatureOutputFormat.class);

    @Autowired private Jaxb2Marshaller mapmlMarshaller;

    private String base;
    private String path;
    private Map<String, Object> query;

    /** @param gs the GeoServer instance */
    public MapMLGetFeatureOutputFormat(GeoServer gs) {
        super(gs, MapMLConstants.FORMAT_NAME);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MapMLConstants.MAPML_MIME_TYPE + ";charset=UTF-8";
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollectionResponse,
            OutputStream out,
            Operation getFeature)
            throws IOException, ServiceException {

        List<FeatureCollection> featureCollections = featureCollectionResponse.getFeatures();
        if (featureCollections.size() != 1) {
            throw new ServiceException(
                    "MapML OutputFormat does not support Multiple Feature Type output.");
        }
        FeatureCollection featureCollection = featureCollections.get(0);
        if (!(featureCollection instanceof SimpleFeatureCollection)) {
            throw new ServiceException("MapML OutputFormat does not support Complex Features.");
        }
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;
        LayerInfo layerInfo = gs.getCatalog().getLayerByName(fc.getSchema().getTypeName());
        URI srsName = getSrsNameFromOperation(getFeature);
        CoordinateReferenceSystem requestCRS = null;
        try {
            requestCRS = srsName != null ? CRS.decode(srsName.toString()) : null;
        } catch (FactoryException e) {
            LOGGER.warning("Could not decode SRS name: " + e.getMessage());
        }
        int numDecimals = this.getNumDecimals(featureCollections, gs, gs.getCatalog());
        boolean forcedDecimal = this.getForcedDecimal(featureCollections, gs, gs.getCatalog());
        boolean padWithZeros = this.getPadWithZeros(featureCollections, gs, gs.getCatalog());
        Mapml mapml =
                MapMLFeatureUtil.featureCollectionToMapML(
                        featureCollection,
                        layerInfo,
                        requestCRS,
                        MapMLFeatureUtil.alternateProjections(this.base, this.path, this.query),
                        numDecimals,
                        forcedDecimal,
                        padWithZeros,
                        null);

        // write to output
        OutputStreamWriter osw = new OutputStreamWriter(out, gs.getSettings().getCharset());
        Result result = new StreamResult(osw);
        mapmlMarshaller.marshal(mapml, result);
        osw.flush();
    }

    /**
     * @param getFeature the Operation from which we get the srsName of the request
     * @return the EPSG / whatever code that was requested, or null if not available
     */
    private URI getSrsNameFromOperation(Operation getFeature) {
        URI srsName = null;
        try {
            boolean wfs1 = getFeature.getService().getVersion().toString().startsWith("1");
            srsName =
                    wfs1
                            ? ((QueryTypeImpl)
                                            ((GetFeatureTypeImpl) getFeature.getParameters()[0])
                                                    .getQuery()
                                                    .get(0))
                                    .getSrsName()
                            : ((net.opengis.wfs20.impl.QueryTypeImpl)
                                            ((net.opengis.wfs20.impl.GetFeatureTypeImpl)
                                                            getFeature.getParameters()[0])
                                                    .getAbstractQueryExpressionGroup()
                                                    .get(0)
                                                    .getValue())
                                    .getSrsName();
        } catch (Exception e) {
            LOGGER.warning("Could not get SRS name from Operation: " + e.getMessage());
        }
        return srsName;
    }

    /**
     * Set the base context of the URL of the request for use in output format; set by callback
     *
     * @param base the URL to use as base, corresponds to ResponseUtils.buildURL base parameter
     */
    public void setBase(String base) {
        this.base = base;
    }

    /**
     * Set the query part of the URL to use as input to ResponseUtils.buildURL
     *
     * @param query the query part of the URL
     */
    public void setQuery(Map<String, Object> query) {
        this.query = query;
    }

    /**
     * Set the path to be used as the path parameter for input to ResponseUtils.buildURL
     *
     * @param path the path to use
     */
    public void setPath(String path) {
        this.path = path;
    }
}
