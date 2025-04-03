package org.geoserver.wms.vector;

import static org.junit.Assert.assertTrue;

import no.ecc.vectortile.VectorTileEncoderNoClip;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

// Verify vector tile clipping works as expected - geometries that lie beyond the tile bounds & clip buffer should not
// be clipped as we assume upstream has already clipped the geometries
public class VectorTileEncoderNoClipTest {

    @Test
    public void testNoClippingApplied() {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        // Position line outside of the clipBuffer (100)
        VectorTileEncoderNoClip encoder = new VectorTileEncoderNoClip(4096, 100, false);
        org.locationtech.jts.geom.LineString lineOutsideBounds = geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(-150, 0),
                new Coordinate(-150, 150)
        });
        encoder.addFeature("ClipTestLayer", new java.util.HashMap<>(), lineOutsideBounds);
        byte[] tile = encoder.encode();
        // Ensure tile is generated and line has not been clipped (=tile is not empty)
        assertTrue("Line should not be clipped", tile.length > 0);

        // Position point outside of the clipBuffer (100)
        encoder = new VectorTileEncoderNoClip(4096, 100, false);
        org.locationtech.jts.geom.Point pointOutsideBounds = geometryFactory.createPoint(new Coordinate(-150, 0));
        encoder.addFeature("ClipTestLayer", new java.util.HashMap<>(), pointOutsideBounds);
        tile = encoder.encode();
        // Ensure tile is generated and point has not been clipped (=tile is not empty)
        assertTrue("Point should not be clipped", tile.length > 0);
    }
}
