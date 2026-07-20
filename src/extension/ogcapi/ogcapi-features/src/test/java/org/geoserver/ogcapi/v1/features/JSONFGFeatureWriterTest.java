/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;

public class JSONFGFeatureWriterTest {

    private static GeoServer gs;

    static {
        gs = new GeoServerImpl();
        gs.setCatalog(new CatalogImpl());
    }

    JSONFGFeatureWriter toTest = new JSONFGFeatureWriter(gs, null, null) {
        @Override
        protected boolean isFeatureBounding() {
            return false;
        }
    };

    private List<FeatureCollection<SimpleFeatureType, SimpleFeature>> geoCollection()
            throws SchemaException, ParseException {
        SimpleFeatureType TYPE = DataUtilities.createType("location", "geom:Point:srid=4326,name:String");
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection("internal", TYPE);

        WKTReader2 wkt = new WKTReader2();

        featureCollection.add(SimpleFeatureBuilder.build(TYPE, new Object[] {wkt.read("POINT(1 2)"), "poi1"}, null));
        featureCollection.add(SimpleFeatureBuilder.build(TYPE, new Object[] {wkt.read("POINT(4 4)"), "poi2"}, null));

        return List.of(featureCollection);
    }

    private List<FeatureCollection<SimpleFeatureType, SimpleFeature>> nonGeoCollection()
            throws SchemaException, ParseException {
        SimpleFeatureType TYPE = DataUtilities.createType("places", "name:String,population:Integer");
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection("internal", TYPE);

        featureCollection.add(SimpleFeatureBuilder.build(TYPE, new Object[] {"Canada", 40_518_860}, null));
        featureCollection.add(SimpleFeatureBuilder.build(TYPE, new Object[] {"France", 69_082_000}, null));

        return List.of(featureCollection);
    }

    @Test
    public void testFeatureWriterGeo() throws SchemaException, ParseException, IOException {
        OutputStream out = new ByteArrayOutputStream();

        toTest.write(geoCollection(), out, BigInteger.TEN, false);

        JsonContext json = (JsonContext) JsonPath.parse(out.toString());
        assertEquals("FeatureCollection", json.read("$.type", String.class));
    }

    @Test
    public void testFeatureWriterNonGeo() throws SchemaException, IOException, ParseException {
        OutputStream out = new ByteArrayOutputStream();

        toTest.write(nonGeoCollection(), out, BigInteger.TEN, false);

        assertEquals("FeatureCollection", JsonPath.parse(out.toString()).read("$.type", String.class));
        assertNull(JsonPath.parse(out.toString()).read("$.features[0].geometry", String.class));
    }
}
