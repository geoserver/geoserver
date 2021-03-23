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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.mapml.tcrs.TiledCRSParams;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
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
        // the MapMLController serialized the CRS parameter, so we can rely
        // on it being a string match for TiledCRSParamms.code field.
        String crs = request.getRawKvp().get("CRS");
        String projection = null;
        for (TiledCRSParams tcrs : TiledCRSConstants.tiledCRSDefinitions.values()) {
            if (tcrs.getCode().equalsIgnoreCase(crs)) {
                projection = tcrs.getName();
                break;
            }
        }

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
                                    MapMLGenerator.buildFeature(
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
}
