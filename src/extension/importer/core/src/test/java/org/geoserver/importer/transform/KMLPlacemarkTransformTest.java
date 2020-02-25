/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.Folder;
import org.geotools.styling.FeatureTypeStyle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public class KMLPlacemarkTransformTest extends TestCase {

    private KMLPlacemarkTransform kmlPlacemarkTransform;

    private SimpleFeatureType origType;

    private SimpleFeatureType transformedType;

    @Override
    protected void setUp() throws Exception {
        kmlPlacemarkTransform = new KMLPlacemarkTransform();

        SimpleFeatureTypeBuilder origBuilder = new SimpleFeatureTypeBuilder();
        origBuilder.setName("origtype");
        origBuilder.add("name", String.class);
        origBuilder.add("description", String.class);
        origBuilder.add("LookAt", Point.class);
        origBuilder.add("Region", LinearRing.class);
        origBuilder.add("Style", FeatureTypeStyle.class);
        origBuilder.add("Geometry", Geometry.class);
        origBuilder.setDefaultGeometry("Geometry");
        origType = origBuilder.buildFeatureType();

        SimpleFeatureTypeBuilder transformedBuilder = new SimpleFeatureTypeBuilder();
        transformedBuilder.setName("transformedtype");
        transformedBuilder.add("name", String.class);
        transformedBuilder.add("description", String.class);
        transformedBuilder.add("LookAt", Point.class);
        transformedBuilder.add("Region", LinearRing.class);
        transformedBuilder.add("Style", String.class);
        transformedBuilder.add("Geometry", Geometry.class);
        transformedBuilder.setDefaultGeometry("Geometry");
        transformedBuilder.add("Folder", String.class);
        transformedType = transformedBuilder.buildFeatureType();
    }

    public void testFeatureType() throws Exception {
        SimpleFeatureType result = kmlPlacemarkTransform.convertFeatureType(origType);
        assertBinding(result, "LookAt", Point.class);
        assertBinding(result, "Region", LinearRing.class);
        assertBinding(result, "Folder", String.class);
    }

    private void assertBinding(SimpleFeatureType ft, String attr, Class<?> expectedBinding) {
        AttributeDescriptor descriptor = ft.getDescriptor(attr);
        Class<?> binding = descriptor.getType().getBinding();
        assertEquals(expectedBinding, binding);
    }

    public void testGeometry() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        GeometryFactory gf = new GeometryFactory();
        fb.set("Geometry", gf.createPoint(new Coordinate(3d, 4d)));
        SimpleFeature feature = fb.buildFeature("testgeometry");
        assertEquals(
                "Unexpected Geometry class",
                Point.class,
                feature.getAttribute("Geometry").getClass());
        assertEquals(
                "Unexpected default geometry",
                Point.class,
                feature.getDefaultGeometry().getClass());
        SimpleFeature result = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        assertEquals(
                "Invalid Geometry class", Point.class, result.getAttribute("Geometry").getClass());
        assertEquals(
                "Unexpected default geometry",
                Point.class,
                feature.getDefaultGeometry().getClass());
    }

    public void testLookAtProperty() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        GeometryFactory gf = new GeometryFactory();
        Coordinate c = new Coordinate(3d, 4d);
        fb.set("LookAt", gf.createPoint(c));
        SimpleFeature feature = fb.buildFeature("testlookat");
        assertEquals(
                "Unexpected LookAt attribute class",
                Point.class,
                feature.getAttribute("LookAt").getClass());
        SimpleFeature result = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        assertEquals(
                "Invalid LookAt attribute class",
                Point.class,
                result.getAttribute("LookAt").getClass());
    }

    public void testFolders() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        List<Folder> folders = new ArrayList<Folder>(2);
        folders.add(new Folder("foo"));
        folders.add(new Folder("bar"));
        fb.featureUserData("Folder", folders);
        SimpleFeature feature = fb.buildFeature("testFolders");
        SimpleFeature newFeature = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        assertEquals("foo -> bar", newFeature.getAttribute("Folder"));
    }
}
