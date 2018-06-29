/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;

public class GMLFileFormatTest {

    private GMLFileFormat gmlFileFormat;

    @Before
    public void setUp() throws Exception {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        gmlFileFormat = new GMLFileFormat();
    }

    @Test
    public void testParsePoiGML2() throws Exception {
        File file =
                new File("./src/test/resources/org/geoserver/importer/test-data/gml/poi.gml2.gml");
        SimpleFeatureType schema = gmlFileFormat.getSchema(file);
        assertEquals(Point.class, schema.getGeometryDescriptor().getType().getBinding());
        assertEquals(
                CRS.decode("EPSG:4326", true),
                schema.getGeometryDescriptor().getType().getCoordinateReferenceSystem());
        assertEquals(String.class, schema.getDescriptor("NAME").getType().getBinding());
        assertEquals(Integer.class, schema.getDescriptor("intAttribute").getType().getBinding());
        assertEquals(Double.class, schema.getDescriptor("floatAttribute").getType().getBinding());
    }

    @Test
    public void testParsePoiGML3() throws Exception {
        File file =
                new File("./src/test/resources/org/geoserver/importer/test-data/gml/poi.gml3.gml");
        SimpleFeatureType schema = gmlFileFormat.getSchema(file);
        assertEquals(Point.class, schema.getGeometryDescriptor().getType().getBinding());
        assertEquals(
                CRS.decode("urn:x-ogc:def:crs:EPSG:4326", false),
                schema.getGeometryDescriptor().getType().getCoordinateReferenceSystem());
        assertEquals(String.class, schema.getDescriptor("NAME").getType().getBinding());
        assertEquals(Integer.class, schema.getDescriptor("intAttribute").getType().getBinding());
        assertEquals(Double.class, schema.getDescriptor("floatAttribute").getType().getBinding());
    }

    @Test
    public void testParseStreamsGML2() throws Exception {
        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/gml/streams.gml2.gml");
        SimpleFeatureType schema = gmlFileFormat.getSchema(file);
        assertEquals(MultiLineString.class, schema.getGeometryDescriptor().getType().getBinding());
        assertEquals(
                CRS.decode("EPSG:26713"),
                schema.getGeometryDescriptor().getType().getCoordinateReferenceSystem());
        assertEquals(String.class, schema.getDescriptor("cat").getType().getBinding());
        assertEquals(Date.class, schema.getDescriptor("acquired").getType().getBinding());
        assertEquals(Date.class, schema.getDescriptor("acquiredFull").getType().getBinding());
    }

    @Test
    public void testParseStreamsGML3() throws Exception {
        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/gml/streams.gml3.gml");
        SimpleFeatureType schema = gmlFileFormat.getSchema(file);
        assertEquals(MultiLineString.class, schema.getGeometryDescriptor().getType().getBinding());
        assertEquals(
                CRS.decode("urn:x-ogc:def:crs:EPSG:26713"),
                schema.getGeometryDescriptor().getType().getCoordinateReferenceSystem());
        assertEquals(Integer.class, schema.getDescriptor("cat").getType().getBinding());
    }
}
