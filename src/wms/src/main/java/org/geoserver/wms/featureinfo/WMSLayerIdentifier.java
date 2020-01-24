/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.clip.ClippedFeatureCollection;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.map.WMSLayer;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v1_0.WFSConfiguration_1_0;
import org.geotools.xsd.Parser;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Layer identifier specialized in WMS cascading layers
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WMSLayerIdentifier implements LayerIdentifier<FeatureCollection> {

    static final Logger LOGGER = Logging.getLogger(WMSLayerIdentifier.class);

    private EntityResolverProvider resolverProvider;

    // WMS service configuration facade, maybe be NULL use method getWms()
    private WMS wms;

    public WMSLayerIdentifier(EntityResolverProvider resolverProvider, WMS wms) {
        this.resolverProvider = resolverProvider;
        this.wms = wms;
    }

    public List<FeatureCollection> identify(FeatureInfoRequestParameters params, int maxFeatures)
            throws IOException {
        final int x = params.getX();
        final int y = params.getY();
        WMSLayerInfo info = (WMSLayerInfo) params.getLayer().getResource();
        WebMapServer wms = info.getStore().getWebMapServer(null);
        Layer layer = info.getWMSLayer(null);

        CoordinateReferenceSystem crs = params.getRequestedCRS();
        if (crs == null) {
            // use the native one
            crs = info.getCRS();
        }
        ReferencedEnvelope bbox = params.getRequestedBounds();
        int width = params.getWidth();
        int height = params.getHeight();

        // we can cascade GetFeatureInfo on queryable layers and if the GML mime type is supported
        if (!layer.isQueryable()) {
            return null;
        }

        List<String> infoFormats;
        infoFormats = wms.getCapabilities().getRequest().getGetFeatureInfo().getFormats();
        if (!infoFormats.contains("application/vnd.ogc.gml")) {
            return null;
        }

        // the wms layer does request in a CRS that's compatible with the WMS server srs
        // list,
        // we may need to transform
        WMSLayer ml = new WMSLayer(wms, layer);
        // delegate to the web map layer as there's quite a bit of reprojection magic
        // code
        // that we want to be consistently reproduced for GetFeatureInfo as well
        final InputStream is =
                ml.getFeatureInfo(
                        bbox, width, height, x, y, "application/vnd.ogc.gml", maxFeatures);
        List<FeatureCollection> results = new ArrayList<FeatureCollection>();
        try {
            Parser parser = new Parser(new WFSConfiguration_1_0());
            parser.setStrict(false);
            parser.setEntityResolver(resolverProvider.getEntityResolver());
            Object result = parser.parse(is);
            if (result instanceof FeatureCollectionType) {
                FeatureCollectionType fcList = (FeatureCollectionType) result;
                List<SimpleFeatureCollection> rawResults = fcList.getFeature();

                // retyping feature collections to replace name and namespace
                // from cascading server with our local WMSLayerInfo
                for (SimpleFeatureCollection fc : rawResults) {
                    SimpleFeatureType ft = fc.getSchema();

                    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                    builder.init(ft);

                    builder.setName(info.getName());
                    builder.setNamespaceURI(info.getNamespace().getURI());

                    SimpleFeatureType targetFeatureType = builder.buildFeatureType();
                    FeatureCollection rfc =
                            handleClipParam(
                                    params, new ReTypingFeatureCollection(fc, targetFeatureType));

                    // if possible force a CRS to be defined
                    results.add(forceCrs(rfc));
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Tried to parse GML2 response, but failed", t);
        } finally {
            is.close();
        }

        // let's see if we need to reproject
        if (!getWms().isFeaturesReprojectionDisabled()) {
            // try to reproject to target CRS
            return LayerIdentifierUtils.reproject(results, params.getRequestedCRS());
        }

        // reprojection no allowed
        return results;
    }

    public boolean canHandle(MapLayerInfo layer) {
        return layer.getType() == MapLayerInfo.TYPE_WMS;
    }

    /**
     * Helper method that tries to force a CRS to be defined. If no CRS is defined buf if all the
     * feature collection geometries use the same CRS that CRS will be forced. This only work for
     * simple features.
     */
    private FeatureCollection forceCrs(FeatureCollection featureCollection) {
        if (featureCollection.getSchema().getCoordinateReferenceSystem() != null) {
            // a CRS is already defined
            return featureCollection;
        }
        // try to extract a CRS from the feature collection features
        CoordinateReferenceSystem crs = LayerIdentifierUtils.getCrs(featureCollection);
        if (crs == null) {
            // there is nothing more we can do
            return featureCollection;
        }
        try {
            // force the CRS
            return new ForceCoordinateSystemFeatureResults(featureCollection, crs);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error forcing feature collection to use SRS '%s'.", CRS.toSRS(crs)),
                    exception);
        }
    }

    /**
     * Does a lookup on the application context if needed.
     *
     * @return WMS service configuration facade
     */
    private WMS getWms() {
        if (wms == null) {
            // no need for synchronization here
            wms = GeoServerExtensions.bean(WMS.class);
        }
        return wms;
    }

    public FeatureCollection<FeatureType, Feature> handleClipParam(
            FeatureInfoRequestParameters params, FeatureCollection fc) {
        Geometry clipGeometry = params.getGetMapRequest().getClip();
        if (clipGeometry == null) return fc;

        return new ClippedFeatureCollection(fc, clipGeometry);
    }
}
