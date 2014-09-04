/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import java.util.Arrays;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class CSVSpecifiedWKTStrategy extends AbstractCSVStrategy implements CSVStrategy {

    private final String wktField;

    public CSVSpecifiedWKTStrategy(CSVFileState csvFileState, String wktField) {
        super(csvFileState);
        this.wktField = wktField;
    }

    @Override
    protected SimpleFeatureType buildFeatureType() {
        SimpleFeatureTypeBuilder builder = CSVStrategySupport.createBuilder(csvFileState);
        String[] csvHeaders = csvFileState.getCSVHeaders();
        List<String> headers = Arrays.asList(csvHeaders);
        if (headers.contains(wktField)) {
            builder.remove(wktField);
            builder.add(wktField, Geometry.class);
        }
        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeature createFeature(String recordId, String[] csvRecord) {
        SimpleFeatureType featureType = getFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        String[] headers = csvFileState.getCSVHeaders();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (i < csvRecord.length) {
                String value = csvRecord[i].trim();
                if (geometryDescriptor != null && header.equals(wktField)) {
                    WKTReader wktReader = new WKTReader();
                    Geometry geometry;
                    try {
                        geometry = wktReader.read(value);
                    } catch (ParseException e) {
                        // policy decision here that just nulls out unparseable geometry
                        geometry = null;
                    }
                    builder.set(wktField, geometry);
                } else {
                    builder.set(header, value);
                }
            } else {
                builder.set(header, null);
            }
        }
        return builder.buildFeature(csvFileState.getTypeName() + "-" + recordId);
    }

}
