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
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.data.ows.Layer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.data.wms.WebMapServer;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.WMSLayer;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v1_0.WFSConfiguration;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Layer identifier specialized in WMS cascading layers
 * 
 * @author Andrea Aime - GeoSolutions 
 */
public class WMSLayerIdentifier implements LayerIdentifier {
    
    static final Logger LOGGER = Logging.getLogger(WMSLayerIdentifier.class);

    private EntityResolverProvider resolverProvider;

    public WMSLayerIdentifier(EntityResolverProvider resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    public List<FeatureCollection> identify(FeatureInfoRequestParameters params, int maxFeatures) throws IOException {
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
        final InputStream is = ml.getFeatureInfo(bbox, width, height, x, y,
                "application/vnd.ogc.gml", maxFeatures);
        List<FeatureCollection> results = new ArrayList<FeatureCollection>();
        try {
            Parser parser = new Parser(new WFSConfiguration());
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
                    FeatureCollection rfc = new ReTypingFeatureCollection(fc, targetFeatureType);
                    
                    results.add(rfc);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Tried to parse GML2 response, but failed", t);
        } finally {
            is.close();
        }
        return results;
    }

    public boolean canHandle(MapLayerInfo layer) {
        return layer.getType() == MapLayerInfo.TYPE_WMS;
    }

}
