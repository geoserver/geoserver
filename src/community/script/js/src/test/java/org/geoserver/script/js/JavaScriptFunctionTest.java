/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.geoserver.script.function.ScriptFunctionFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.URLs;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;

public class JavaScriptFunctionTest extends ScriptIntTestSupport {

    ScriptFunctionFactory functionFactory;

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        File fromDir = URLs.urlToFile(getClass().getResource("scripts"));
        File toDir = new File(dataDirectory.getDataDirectoryRoot(), "scripts");
        IOUtils.deepCopy(fromDir, toDir);
        super.populateDataDirectory(dataDirectory);
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        functionFactory = new ScriptFunctionFactory(getScriptManager());
    }

    @SuppressWarnings("unchecked")
    public void testFactorial() {
        Function factorial = functionFactory.function("factorial", Collections.EMPTY_LIST, null);
        assertNotNull(factorial);
        assertEquals(120, ((Number) factorial.evaluate(5)).intValue());
        // confirm we can do repeat calls
        assertEquals(720, ((Number) factorial.evaluate(6)).intValue());
    }

    public void testBuffer() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(null);
        builder.add("geometry", Polygon.class);
        builder.add("distance", Float.class);
        builder.setName("schema");
        SimpleFeatureType schema = builder.buildFeatureType();

        Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(0, 0);
        coords[1] = new Coordinate(10, 0);
        coords[2] = new Coordinate(10, 10);
        coords[3] = new Coordinate(0, 10);
        coords[4] = new Coordinate(0, 0);

        Object[] attributes = new Object[2];
        GeometryFactory gf = new GeometryFactory(new PrecisionModel());
        LinearRing ring = gf.createLinearRing(coords);
        attributes[0] = gf.createPolygon(ring, null);
        attributes[1] = new Float(100.0);

        SimpleFeature feature = SimpleFeatureBuilder.build(schema, attributes, null);

        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
        PropertyName geometryProperty = filterFactory.property("geometry");
        PropertyName distanceProperty = filterFactory.property("distance");

        Expression[] expressions = new Expression[] {geometryProperty, distanceProperty};
        Function buffer = functionFactory.function("buffer", Arrays.asList(expressions), null);

        assertNotNull(buffer);
        Object result = buffer.evaluate(feature);
        assertTrue(result instanceof Polygon);
        assertTrue(((Polygon) result).getArea() > 35314);
    }
}
