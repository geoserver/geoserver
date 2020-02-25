/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.topojson;

import com.google.common.collect.ImmutableList;
import java.awt.geom.AffineTransform;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.output.NullOutputStream;
import org.geoserver.wms.topojson.TopoGeom.GeometryColleciton;
import org.junit.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class TopoJSONEncoderTest {

    @Test
    public void testIdentity() throws Exception {

        TopoJSONEncoder encoder = new TopoJSONEncoder();

        AffineTransform identity = new AffineTransform();

        List<LineString> arcs = arcs("LINESTRING(0 1, 2 3)");

        Collection<? extends TopoGeom> geoms =
                ImmutableList.of(new TopoGeom.LineString(ImmutableList.of(1, 2, 3, 4, 5)));
        GeometryColleciton layer = new GeometryColleciton(geoms);

        Map<String, GeometryColleciton> layers = new HashMap<>();
        layers.put("topp:states", layer);

        Topology topology = new Topology(identity, arcs, layers);
        Writer writer = new OutputStreamWriter(new NullOutputStream());
        encoder.encode(topology, writer);
    }

    @Test
    public void testQuantization() throws Exception {

        TopoJSONEncoder encoder = new TopoJSONEncoder();

        final double translateX = 100d;
        final double translateY = 200d;

        final double scaleX = 0.001;
        final double scaleY = 0.01;

        AffineTransform tx = new AffineTransform();
        tx.translate(translateX, translateY);
        tx.scale(scaleX, scaleY);

        List<LineString> arcs = arcs("LINESTRING(0 1, 2 3)");

        Collection<? extends TopoGeom> geoms =
                ImmutableList.of(new TopoGeom.LineString(ImmutableList.of(1, 2, 3, 4, 5)));
        GeometryColleciton layer = new GeometryColleciton(geoms);

        Map<String, GeometryColleciton> layers = new HashMap<>();
        layers.put("topp:states", layer);
        Topology topology = new Topology(tx, arcs, layers);

        Writer writer = new OutputStreamWriter(new NullOutputStream());
        encoder.encode(topology, writer);
    }

    private List<LineString> arcs(String... WKT) throws ParseException {
        List<LineString> arcs = new ArrayList<>();
        WKTReader reader = new WKTReader();
        for (String wktString : WKT) {
            LineString l = (LineString) reader.read(wktString);
            arcs.add(l);
        }
        return arcs;
    }
}
