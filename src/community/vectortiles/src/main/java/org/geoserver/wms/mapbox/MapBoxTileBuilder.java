package org.geoserver.wms.mapbox;

import static org.geoserver.wms.mapbox.MapBoxTileBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;

import no.ecc.vectortile.VectorTileEncoder;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * 
 * @author Niels Charlier
 *
 */
public class MapBoxTileBuilder implements VectorTileBuilder {

    private VectorTileEncoder encoder;

    public MapBoxTileBuilder(Rectangle mapSize, ReferencedEnvelope mapArea) {
        final int extent = Math.max(mapSize.width, mapSize.height);
        this.encoder = new VectorTileEncoder(extent, extent / 32, false);
    }

    @Override
    public void addFeature(String layerName, String featureId, String geometryName,
            Geometry geometry, Map<String, Object> properties) {

        encoder.addFeature(layerName, properties, geometry);
    }

    @Override
    public WebMap build(WMSMapContent mapContent) throws IOException {
        byte[] contents = encoder.encode();
        return new RawMap(mapContent, contents, MIME_TYPE);
    }

}
