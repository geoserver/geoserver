/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mapbox;

import static org.geoserver.wms.mapbox.MapBoxTileBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ecc.vectortile.VectorTileEncoder;
import no.ecc.vectortile.VectorTileEncoderNoClip;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/** @author Niels Charlier */
public class MapBoxTileBuilder implements VectorTileBuilder {
    private static final Logger LOGGER = Logging.getLogger(MapBoxTileBuilder.class);

    private VectorTileEncoder encoder;

    public MapBoxTileBuilder(Rectangle mapSize, ReferencedEnvelope mapArea) {
        final int extent = Math.max(mapSize.width, mapSize.height);
        final int polygonClipBuffer = extent / 32;
        final boolean autoScale = false;
        this.encoder = new VectorTileEncoderNoClip(extent, polygonClipBuffer, autoScale);
    }

    @Override
    public void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry geometry,
            Map<String, Object> properties) {
        // Feature ids are "typeName.number", plain numbers or text. Extract the trailing numeric part and make
        // sure it can fit into a long, otherwise we will encode the feature without an id.
        String tail = featureId.substring(featureId.lastIndexOf('.') + 1);
        boolean numeric = !tail.isEmpty() && tail.length() <= 18 && tail.chars().allMatch(c -> c >= '0' && c <= '9');
        if (!numeric && LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Feature id " + featureId + " is not numeric, the feature is encoded without an id");
        }
        encoder.addFeature(layerName, properties, geometry, numeric ? Long.parseLong(tail) : -1);
    }

    @Override
    public RawMap build(WMSMapContent mapContent) throws IOException {
        byte[] contents = encoder.encode();
        return new RawMap(mapContent, contents, MIME_TYPE);
    }
}
