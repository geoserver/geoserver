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

public class CSVSpecifiedLatLngStrategy extends AbstractCSVStrategy implements CSVStrategy {

    private final String latField;

    private final String lngField;

    private final String pointField;

    public CSVSpecifiedLatLngStrategy(CSVFileState csvFileState, String latField, String lngField,
            String pointField) {
        super(csvFileState);
        this.latField = latField;
        this.lngField = lngField;
        this.pointField = pointField;
    }

    public CSVSpecifiedLatLngStrategy(CSVFileState csvFileState, String latField, String lngField) {
        this(csvFileState, latField, lngField, "location");
    }

    @Override
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
        Class<?> latClass = typesFromData.get(latField);
        Class<?> lngClass = typesFromData.get(lngField);
        if (CSVStrategySupport.isNumeric(latClass) && CSVStrategySupport.isNumeric(lngClass)) {
            builder.remove(latField);
            builder.remove(lngField);
            builder.add(pointField, Point.class);
        }
        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeature createFeature(String recordId, String[] csvRecord) {
        SimpleFeatureType featureType = getFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        GeometryFactory geometryFactory = new GeometryFactory();
        Double lat = null, lng = null;
        String[] headers = csvFileState.getCSVHeaders();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (i < csvRecord.length) {
                String value = csvRecord[i].trim();
                if (geometryDescriptor != null && header.equals(latField)) {
                    lat = Double.valueOf(value);
                } else if (geometryDescriptor != null && header.equals(lngField)) {
                    lng = Double.valueOf(value);
                } else {
                    builder.set(header, value);
                }
            } else {
                builder.set(header, null);
            }
        }
        if (geometryDescriptor != null && lat != null && lng != null) {
            Coordinate coordinate = new Coordinate(lat, lng);
            Point point = geometryFactory.createPoint(coordinate);
            builder.set(geometryDescriptor.getLocalName(), point);
        }
        return builder.buildFeature(csvFileState.getTypeName() + "-" + recordId);
    }

}
