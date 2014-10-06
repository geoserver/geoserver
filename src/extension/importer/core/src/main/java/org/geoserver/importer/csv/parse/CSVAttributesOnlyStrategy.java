/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class CSVAttributesOnlyStrategy extends AbstractCSVStrategy implements CSVStrategy {

    public CSVAttributesOnlyStrategy(CSVFileState csvFileState) {
        super(csvFileState);
    }

    protected SimpleFeatureType buildFeatureType() {
        SimpleFeatureTypeBuilder builder = CSVStrategySupport.createBuilder(csvFileState);
        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeature createFeature(String recordId, String[] csvRecord) {
        SimpleFeatureType featureType = getFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        String[] headers;
        headers = csvFileState.getCSVHeaders();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (i < csvRecord.length) {
                String value = csvRecord[i].trim();
                builder.set(header, value);
            } else {
                // geotools converters take care of converting for us
                builder.set(header, null);
            }
        }
        return builder.buildFeature(csvFileState.getTypeName() + "-" + recordId);
    }

}
