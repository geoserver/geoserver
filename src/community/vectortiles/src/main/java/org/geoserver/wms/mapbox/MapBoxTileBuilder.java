package org.geoserver.wms.mapbox;

import static org.geoserver.wms.mapbox.MapBoxTileBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import no.ecc.vectortile.VectorTileEncoder;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * 
 * @author Niels Charlier
 *
 */
public class MapBoxTileBuilder implements VectorTileBuilder {

    private VectorTileEncoder encoder;

    public MapBoxTileBuilder(Rectangle mapSize, ReferencedEnvelope mapArea, boolean forceCrs) {
        final int extent = Math.max(mapSize.width, mapSize.height);
        this.encoder = new VectorTileEncoder(extent, extent / 32, false);
    }

    @Override
    public void addFeature(SimpleFeature feature) {
        String layerName = feature.getFeatureType().getTypeName();
        Map<String, ?> attributes = propertiesToAttributes(feature.getProperties());
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        encoder.addFeature(layerName, attributes, geometry);
    }

    protected Map<String, Object> propertiesToAttributes(Collection<Property> properties) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        Iterator<Property> it = properties.iterator();
        while (it.hasNext()) {
            Property property = it.next();
            if (!(property.getValue() instanceof Geometry)) {
                attributes.put(property.getName().getLocalPart(), property.getValue());
            }
        }
        return attributes;
    }

    @Override
    public WebMap build(WMSMapContent mapContent) throws IOException {

        byte[] contents = encoder.encode();
        return new RawMap(mapContent, contents, MIME_TYPE);
    }

}
