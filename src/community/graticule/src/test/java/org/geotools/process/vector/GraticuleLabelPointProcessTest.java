/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.process.vector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.logging.Level;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Point;

public class GraticuleLabelPointProcessTest extends GraticuleLabelTestSupport {

    @Test
    public void testBothLabelGrid() throws Exception {
        String pos = "both";
        ReferencedEnvelope box = bounds;
        SimpleFeatureCollection features = runLabels(box, pos);
    }

    @Test
    public void testBottomBigBBox() throws Exception {
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        -260.15625, 279.84375, -97.734375, 172.265625, DefaultGeographicCRS.WGS84);
        SimpleFeatureCollection features = runLabels(bbox, "bottom");
        checkLabels(features, "bottom");
    }

    @Test
    public void testBigBBox() throws Exception {
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        -260.15625, 279.84375, -97.734375, 172.265625, DefaultGeographicCRS.WGS84);
        SimpleFeatureCollection features = runLabels(bbox, "both");
        checkLabels(features, "both");
    }

    @Test
    public void testSmallBBox() throws Exception {
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        -92.724609375,
                        -25.224609375,
                        -0.615234375,
                        33.134765625,
                        DefaultGeographicCRS.WGS84);
        runLabels(bbox, "both");
        SimpleFeatureCollection features = runLabels(bbox, "topright");
        checkLabels(features, "topright");
        runLabels(bbox, "bottomLeft");
    }

    private void checkLabels(SimpleFeatureCollection features, String placement) {
        GraticuleLabelPointProcess.PositionEnum pos =
                GraticuleLabelPointProcess.PositionEnum.byName(placement).get();

        boolean left = false, top = false;
        boolean both = false;
        switch (pos) {
            case TOPLEFT:
            case TOP:
            case LEFT:
                left = true;
                top = true;
                break;
            case TOPRIGHT:
                left = false;
                top = true;
                break;
            case BOTTOMLEFT:
                left = true;
                top = false;
                break;
            case BOTTOMRIGHT:
            case BOTTOM:
            case RIGHT:
                left = false;
                top = false;
                break;
            case BOTH:
                both = true;
                top = true;
                left = true;
        }

        try (SimpleFeatureIterator itr = features.features()) {
            while (itr.hasNext()) {
                SimpleFeature f = itr.next();
                if (both) {
                    // for both we get top left, then bottom right in sequence
                    top = !top;
                    left = !left;
                }
                boolean horizontal = (boolean) f.getAttribute("horizontal");
                log.fine(
                        f.getDefaultGeometry()
                                + " left:"
                                + f.getAttribute("left")
                                + " top:"
                                + f.getAttribute("top")
                                + " Horiz:"
                                + horizontal);
                boolean obs = (boolean) f.getAttribute("left");
                if (horizontal) Assert.assertEquals("wrong left", left, obs);
                obs = (boolean) f.getAttribute("top");
                if (!horizontal) Assert.assertEquals("wrong top", top, obs);
            }
        }
    }

    private SimpleFeatureCollection runLabels(ReferencedEnvelope box, String pos)
            throws IOException {
        SimpleFeatureCollection features = store.getFeatureSource("10_0").getFeatures();

        GraticuleLabelPointProcess process = new GraticuleLabelPointProcess();

        SimpleFeatureCollection results = process.execute(features, box, pos);
        // assertEquals(features.size() * 2, results.size());
        try (SimpleFeatureIterator iterator = results.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();

                Point p = (Point) feature.getAttribute("element");
                if (log.getLevel() == Level.FINE) {
                    for (Property prop : feature.getProperties()) {
                        log.fine(prop.toString());
                    }
                }
                boolean top = (boolean) feature.getAttribute("top");
                boolean left = (boolean) feature.getAttribute("left");
                boolean horizontal = (boolean) feature.getAttribute("horizontal");

                if (horizontal) {
                    if (left) {
                        assertEquals(
                                "wrong left",
                                Math.floor(Math.max(bounds.getMinimum(0), box.getMinimum(0))),
                                Math.floor(p.getX()),
                                0.1);
                    }
                    if (!left) {
                        assertEquals(
                                "wrong right",
                                Math.ceil(Math.min(bounds.getMaximum(0), box.getMaximum(0))),
                                Math.ceil(p.getX() + GraticuleLabelPointProcess.DELTA),
                                0.1);
                    }
                } else {
                    if (top) {
                        assertEquals(
                                "wrong top",
                                Math.floor(Math.min(bounds.getMaximum(1), box.getMaximum(1))),
                                Math.floor(p.getY()),
                                0.1);
                    }
                    if (!top) {
                        assertEquals(
                                "wrong bottom",
                                Math.ceil(Math.max(bounds.getMinimum(1), box.getMinimum(1))),
                                Math.ceil(p.getY() + GraticuleLabelPointProcess.DELTA),
                                0.1);
                    }
                }
            }
        }
        return results;
    }
}
