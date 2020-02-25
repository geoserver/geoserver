/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mapbox;

import static org.geoserver.wms.mapbox.MapBoxTileBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;
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
        int id = -1;
        if (featureId.matches(".*\\.[0-9]+")) {
            try {
                id = Integer.parseInt(featureId.split("\\.")[1]);
            } catch (NumberFormatException e) {
            }
        }

        if (id < 0) {
            LOGGER.warning("Cannot obtain numeric id from featureId: " + featureId);
        }

        encoder.addFeature(layerName, properties, geometry, id);
    }

    @Override
    public RawMap build(WMSMapContent mapContent) throws IOException {
        byte[] contents = encoder.encode();
        return new RawMap(mapContent, contents, MIME_TYPE);
    }
}
