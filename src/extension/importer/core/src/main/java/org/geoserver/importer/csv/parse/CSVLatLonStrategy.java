/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import java.io.IOException;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.csvreader.CsvReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CSVLatLonStrategy extends AbstractCSVStrategy implements CSVStrategy {

    private static final String GEOMETRY_COLUMN = "location";

    public CSVLatLonStrategy(CSVFileState csvFileState) {
        super(csvFileState);
    }

    protected SimpleFeatureType buildFeatureType() {
        String[] headers;
        Map<String, Class<?>> typesFromData;
        CsvReader csvReader = null;
        try {
            csvReader = csvFileState.openCSVReader();
            headers = csvReader.getHeaders();
            typesFromData = CSVStrategySupport.findMostSpecificTypesFromData(csvReader, headers);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }
        SimpleFeatureTypeBuilder builder = CSVStrategySupport.createBuilder(csvFileState, headers,
                typesFromData);
        boolean validLat = false;
        boolean validLon = false;
        String latSpelling = null;
        String lonSpelling = null;
        for (String col : headers) {
            Class<?> type = typesFromData.get(col);
            if (isLatitude(col)) {
                latSpelling = col;
                if (CSVStrategySupport.isNumeric(type)) {
                    validLat = true;
                }
            } else if (isLongitude(col)) {
                lonSpelling = col;
                if (CSVStrategySupport.isNumeric(type)) {
                    validLon = true;
                }
            }
        }
        if (validLat && validLon) {
            builder.add(GEOMETRY_COLUMN, Point.class);
            builder.remove(latSpelling);
            builder.remove(lonSpelling);
        }
        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeature createFeature(String recordId, String[] csvRecord) {
        SimpleFeatureType featureType = getFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        GeometryFactory geometryFactory = new GeometryFactory();
        Double x = null, y = null;
        String[] headers = csvFileState.getCSVHeaders();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (i < csvRecord.length) {
                String value = csvRecord[i].trim();
                if (geometryDescriptor != null && isLatitude(header)) {
                    y = Double.valueOf(value);
                } else if (geometryDescriptor != null && isLongitude(header)) {
                    x = Double.valueOf(value);
                } else {
                    builder.set(header, value);
                }
            } else {
                builder.set(header, null);
            }
        }
        if (x != null && y != null && geometryDescriptor != null) {
            Coordinate coordinate = new Coordinate(x, y);
            Point point = geometryFactory.createPoint(coordinate);
            builder.set(geometryDescriptor.getLocalName(), point);
        }
        return builder.buildFeature(csvFileState.getTypeName() + "-" + recordId);
    }

    private boolean isLatitude(String s) {
        return "latitude".equalsIgnoreCase(s) || "lat".equalsIgnoreCase(s);
    }

    private boolean isLongitude(String s) {
        return "lon".equalsIgnoreCase(s) || "lng".equalsIgnoreCase(s) || "long".equalsIgnoreCase(s)
                || "longitude".equalsIgnoreCase(s);
    }
}
