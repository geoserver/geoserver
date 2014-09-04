/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv;

import static org.geoserver.importer.ImporterTestUtils.unpack;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.geoserver.importer.ImporterTestUtils.unpack;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.junit.Before;
import org.junit.Test;
import org.geoserver.importer.csv.parse.CSVLatLonStrategy;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class CSVDataStoreTest {

    private CSVDataStore csvDataStore;

    @Before
    public void setUp() throws Exception {
        URL resource = DataUtilities.fileToURL(new File(unpack("csv/locations.zip"), "locations.csv"));
        assertNotNull("Failure finding locations csv file", resource);
        File file = DataUtilities.urlToFile(resource);
        CSVFileState csvFileState = new CSVFileState(file);
        CSVLatLonStrategy csvStrategy = new CSVLatLonStrategy(csvFileState);
        csvDataStore = new CSVDataStore(csvFileState, csvStrategy);
    }

    @Test
    public void testGetTypeName() {
        Name typeName = csvDataStore.getTypeName();
        assertEquals("Invalid type name", "locations", typeName.getLocalPart());
    }

    private List<Coordinate> makeExpectedCoordinates(double... points) {
        List<Coordinate> result = new ArrayList<Coordinate>(points.length);
        double x = -1;
        for (double d : points) {
            if (x == -1) {
                x = d;
            } else {
                Coordinate coordinate = new Coordinate(d, x);
                x = -1;
                result.add(coordinate);
            }
        }
        return result;
    }

    @Test
    public void testReadFeatures() throws IOException {
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = csvDataStore.getFeatureReader();
        List<Coordinate> geometries = new ArrayList<Coordinate>();
        List<String> cities = new ArrayList<String>();
        List<String> numbers = new ArrayList<String>();

        while (reader.hasNext()) {
            SimpleFeature feature = reader.next();
            Point geometry = (Point) feature.getDefaultGeometry();
            geometries.add(geometry.getCoordinate());
            cities.add(feature.getAttribute("CITY").toString());
            numbers.add(feature.getAttribute("NUMBER").toString());
        }

        List<Coordinate> expectedCoordinates = makeExpectedCoordinates(46.066667, 11.116667,
                44.9441, -93.0852, 13.752222, 100.493889, 45.420833, -75.69, 44.9801, -93.251867,
                46.519833, 6.6335, 48.428611, -123.365556, -33.925278, 18.423889, -33.859972,
                151.211111);
        assertEquals("Unexpected coordinates", expectedCoordinates, geometries);

        List<String> expectedCities = Arrays
                .asList("Trento, St Paul, Bangkok, Ottawa, Minneapolis, Lausanne, Victoria, Cape Town, Sydney"
                        .split(", "));
        assertEquals("Unexecpted cities", expectedCities, cities);

        List<String> expectedNumbers = Arrays.asList("140, 125, 150, 200, 350, 560, 721, 550, 436"
                .split(", "));
        assertEquals("Unexpected numbers", expectedNumbers, numbers);
    }
}
