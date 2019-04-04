/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.builder;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LookAt;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class AbstractNetworkLinkBuilder {

    static final Logger LOGGER = Logging.getLogger(AbstractNetworkLinkBuilder.class);

    protected KmlEncodingContext context;

    public AbstractNetworkLinkBuilder(KmlEncodingContext context) {
        this.context = context;
    }

    public Kml buildKMLDocument() {
        // prepare kml, document and folder
        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();
        Map formatOptions = context.getRequest().getFormatOptions();
        String kmltitle = (String) formatOptions.get("kmltitle");
        if (kmltitle == null) {
            kmltitle = context.getMapContent().getTitle();
        }
        document.setName(kmltitle);

        // get the callbacks for the document and let them loose
        List<KmlDecorator> decorators = context.getDecoratorsForClass(Document.class);
        for (KmlDecorator decorator : decorators) {
            document = (Document) decorator.decorate(document, context);
            if (document == null) {
                throw new ServiceException(
                        "Coding error in decorator "
                                + decorator
                                + ", document objects cannot be set to null");
            }
        }

        encodeDocumentContents(document);

        return kml;
    }

    abstract void encodeDocumentContents(Document document);

    /**
     * @return the aggregated bounds for all the requested layers, taking into account whether the
     *     whole layer or filtered bounds is used for each layer
     */
    protected ReferencedEnvelope computePerLayerQueryBounds(
            final WMSMapContent context,
            final List<ReferencedEnvelope> target,
            final LookAt lookAt) {

        // no need to compute queried bounds if request explicitly specified the view area
        final boolean computeQueryBounds = lookAt == null;

        ReferencedEnvelope aggregatedBounds;
        try {
            boolean longitudeFirst = true;
            aggregatedBounds = new ReferencedEnvelope(CRS.decode("EPSG:4326", longitudeFirst));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        aggregatedBounds.setToNull();

        final List<Layer> mapLayers = context.layers();
        final List<MapLayerInfo> layerInfos = context.getRequest().getLayers();
        for (int i = 0; i < mapLayers.size(); i++) {
            final Layer Layer = mapLayers.get(i);
            final MapLayerInfo layerInfo = layerInfos.get(i);

            ReferencedEnvelope layerLatLongBbox;
            layerLatLongBbox = computeLayerBounds(Layer, layerInfo, computeQueryBounds);
            try {
                layerLatLongBbox =
                        layerLatLongBbox.transform(
                                aggregatedBounds.getCoordinateReferenceSystem(), true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            target.add(layerLatLongBbox);
            aggregatedBounds.expandToInclude(layerLatLongBbox);
        }
        return aggregatedBounds;
    }

    @SuppressWarnings("rawtypes")
    protected ReferencedEnvelope computeLayerBounds(
            Layer layer, MapLayerInfo layerInfo, boolean computeQueryBounds) {

        final Query layerQuery = layer.getQuery();
        // make sure if layer is going to be filtered, the resulting bounds are obtained instead of
        // the whole bounds
        final Filter filter = layerQuery.getFilter();
        if (layerQuery.getFilter() == null || Filter.INCLUDE.equals(filter)) {
            computeQueryBounds = false;
        }
        if (!computeQueryBounds && !layerQuery.isMaxFeaturesUnlimited()) {
            computeQueryBounds = true;
        }

        ReferencedEnvelope layerLatLongBbox = null;
        if (computeQueryBounds) {
            FeatureSource featureSource = layer.getFeatureSource();
            try {
                CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
                FeatureCollection features = featureSource.getFeatures(layerQuery);
                layerLatLongBbox = features.getBounds();
                layerLatLongBbox = layerLatLongBbox.transform(targetCRS, true);
            } catch (Exception e) {
                LOGGER.info(
                        "Error computing bounds for "
                                + featureSource.getName()
                                + " with "
                                + layerQuery);
            }
        }
        if (layerLatLongBbox == null) {
            try {
                layerLatLongBbox = layerInfo.getLatLongBoundingBox();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return layerLatLongBbox;
    }
}
