/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * @author Chris Hodgson
 * @author prushforth
 */
public class MapMLGetFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.mapml");

    @Autowired private Jaxb2Marshaller mapmlMarshaller;

    private WMS wms;

    /** @param wms */
    public MapMLGetFeatureInfoOutputFormat(WMS wms) {
        super(MapMLConstants.MAPML_MIME_TYPE);
        this.wms = wms;
    }

    /**
     * @param results the set of FeatureCollections that respond to the query
     * @param request metadata about the request, including layers maybe involved
     * @param out put the text out on this, return to client.
     * @throws ServiceException
     * @throws IOException
     */
    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {

        String baseUrl = request.getBaseUrl();
        Map<String, String> kvp = request.getRawKvp();
        String projection =
                TiledCRSConstants.lookupTCRSName(
                        kvp.getOrDefault(
                                "CRS",
                                kvp.getOrDefault("SRS", kvp.getOrDefault("TILEMATRIXSET", ""))));

        // build the mapML doc
        Mapml mapml = new Mapml();

        // build the head
        HeadContent head = new HeadContent();
        head.setTitle("GetFeatureInfo Results");
        Base base = new Base();
        base.setHref(ResponseUtils.buildURL(baseUrl, "mapml/", null, URLMangler.URLType.SERVICE));
        head.setBase(base);
        List<Meta> metas = head.getMetas();
        Meta meta = new Meta();
        meta.setCharset("UTF-8");
        metas.add(meta);
        meta = new Meta();
        meta.setHttpEquiv("Content-Type");
        meta.setContent(MapMLConstants.MAPML_MIME_TYPE);
        metas.add(meta);
        Meta tcrsMeta = new Meta();
        tcrsMeta.setName("projection");
        tcrsMeta.setContent(projection);
        Meta csMeta = new Meta();
        // for GetFeatureInfo requests, the coordinate system is always pcrs
        csMeta.setName("cs");
        csMeta.setContent("pcrs");
        metas.add(tcrsMeta);
        metas.add(csMeta);
        mapml.setHead(head);

        // build the body
        BodyContent body = new BodyContent();
        mapml.setBody(body);

        @SuppressWarnings("unchecked")
        List<FeatureCollection> featureCollections = results.getFeature();
        HashMap<Name, String> captionTemplates = captionsMap(request.getQueryLayers());
        SimpleFeatureCollection fc;
        MapMLGenerator featureBuilder = new MapMLGenerator();
        featureBuilder.setNumDecimals(
                getNumDecimals(
                        featureCollections, wms.getGeoServer(), wms.getGeoServer().getCatalog()));
        if (!featureCollections.isEmpty()) {
            Iterator<FeatureCollection> fci = featureCollections.iterator();
            while (fci.hasNext()) {
                fc = (SimpleFeatureCollection) fci.next();
                List<Feature> features = body.getFeatures();
                try (SimpleFeatureIterator iterator = fc.features()) {
                    while (iterator.hasNext()) {
                        SimpleFeature feature;
                        try {
                            // this can throw due to the feature not fitting into
                            // the extent of the CRS when it's transformed. Could
                            // see if in such a case we could just return the
                            // scalar properties
                            feature = iterator.next();
                            Feature f =
                                    featureBuilder.buildFeature(
                                            feature,
                                            captionTemplates.get(fc.getSchema().getName()));
                            // might be interesting to be able to put features
                            // from different layers into a layer-specific div
                            features.add(f);
                        } catch (IllegalStateException e) {
                            LOGGER.log(Level.INFO, "Error transforming feature.");
                        }
                    }
                }
            }
        }

        OutputStreamWriter osw = new OutputStreamWriter(out, wms.getCharSet());
        Result result = new StreamResult(osw);
        mapmlMarshaller.marshal(mapml, result);
        osw.flush();
    }

    @Override
    public String getCharset() {
        // MapML is always encoded as UTF-8; SettingsInfo.getCharset() notwithstanding
        return "UTF-8";
    }

    /**
     * Create a Map wtih key of qualified layer name, to a possibly null, string-valued template to
     * be processed to substitute ${placeholders} with attribute or dimension/band values
     *
     * @param layers a list of all layers that are in the layer / layer group
     * @return
     */
    private HashMap<Name, String> captionsMap(List<MapLayerInfo> layers) {
        HashMap<Name, String> map = new HashMap<>(layers.size());

        layers.stream()
                .map(layer -> layer.getLayerInfo().getResource())
                .forEachOrdered(
                        r -> {
                            String fcap =
                                    r.getMetadata()
                                            .getOrDefault("mapml.featureCaption", "")
                                            .toString();
                            map.put(r.getQualifiedName(), fcap.isEmpty() ? null : fcap);
                        });
        return map;
    }
    /**
     * Copied from org.geoserver.wfs.WFSGetFeatureOutputFormat
     *
     * @param featureCollections
     * @param geoServer
     * @param catalog
     * @return
     */
    protected int getNumDecimals(List featureCollections, GeoServer geoServer, Catalog catalog) {
        int numDecimals = -1;
        for (Object featureCollection : featureCollections) {
            Integer ftiDecimals =
                    getFeatureTypeInfoProperty(
                            catalog,
                            (FeatureCollection) featureCollection,
                            fti -> fti.getNumDecimals());

            // track num decimals, in cases where the query has multiple types we choose the max
            // of all the values (same deal as above, might not be a vector due to GetFeatureInfo
            // reusing this)
            if (ftiDecimals != null && ftiDecimals > 0) {
                numDecimals = numDecimals == -1 ? ftiDecimals : Math.max(numDecimals, ftiDecimals);
            }
        }

        SettingsInfo settings = geoServer.getSettings();

        if (numDecimals == -1) {
            numDecimals = settings.getNumDecimals();
        }

        return numDecimals;
    }
    /**
     * Copied from org.geoserver.wfs.WFSGetFeatureOutputFormat
     *
     * @param <T>
     * @param catalog
     * @param features
     * @param callback
     * @return
     */
    private <T> T getFeatureTypeInfoProperty(
            Catalog catalog, FeatureCollection features, Function<FeatureTypeInfo, T> callback) {
        FeatureTypeInfo fti;
        ResourceInfo meta = null;
        // if it's a complex feature collection get the proper ResourceInfo
        if (features instanceof TypeInfoCollectionWrapper.Complex) {
            TypeInfoCollectionWrapper.Complex fcollection =
                    (TypeInfoCollectionWrapper.Complex) features;
            fti = fcollection.getFeatureTypeInfo();
            meta = catalog.getResourceByName(fti.getName(), ResourceInfo.class);
        } else {
            // no complex, normal behavior
            FeatureType featureType = features.getSchema();
            meta = catalog.getResourceByName(featureType.getName(), ResourceInfo.class);
        }
        if (meta instanceof FeatureTypeInfo) {
            fti = (FeatureTypeInfo) meta;
            return callback.apply(fti);
        }
        return null;
    }
}
