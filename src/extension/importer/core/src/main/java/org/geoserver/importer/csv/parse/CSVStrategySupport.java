/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geoserver.importer.csv.CSVFileState;

import com.csvreader.CsvReader;

public class CSVStrategySupport {

    public static SimpleFeatureTypeBuilder createBuilder(CSVFileState csvFileState) {
        CsvReader csvReader = null;
        Map<String, Class<?>> typesFromData = null;
        String[] headers = null;
        try {
            csvReader = csvFileState.openCSVReader();
            headers = csvReader.getHeaders();
            typesFromData = findMostSpecificTypesFromData(csvReader, headers);
        } catch (IOException e) {
            throw new RuntimeException("Failure reading csv file", e);
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }
        return CSVStrategySupport.createBuilder(csvFileState, headers, typesFromData);
    }

    public static SimpleFeatureTypeBuilder createBuilder(CSVFileState csvFileState,
            String[] headers, Map<String, Class<?>> typesFromData) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(csvFileState.getTypeName());
        builder.setCRS(csvFileState.getCrs());
        if (csvFileState.getNamespace() != null) {
            builder.setNamespaceURI(csvFileState.getNamespace());
        }
        for (String col : headers) {
            Class<?> type = typesFromData.get(col);
            builder.add(col, type);
        }
        return builder;
    }

    public static Map<String, Class<?>> findMostSpecificTypesFromData(CsvReader csvReader,
            String[] headers) throws IOException {
        Map<String, Class<?>> result = new HashMap<String, Class<?>>();
        // start off assuming Integers for everything
        for (String header : headers) {
            result.put(header, Integer.class);
        }
        while (csvReader.readRecord()) {
            String[] record = csvReader.getValues();
            List<String> values = Arrays.asList(record);
            if (record.length >= headers.length) {
                values = values.subList(0, headers.length);
            }
            int i = 0;
            for (String value : values) {
                String header = headers[i];
                Class<?> type = result.get(header);
                if (type == Integer.class) {
                    try {
                        Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        try {
                            Double.parseDouble(value);
                            type = Double.class;
                        } catch (NumberFormatException ex) {
                            type = String.class;
                        }
                    }
                } else if (type == Double.class) {
                    try {
                        Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        type = String.class;
                    }
                } else {
                    type = String.class;
                }
                result.put(header, type);
                i++;
            }
        }
        return result;
    }

    public static boolean isNumeric(Class<?> clazz) {
        return clazz != null && (clazz == Double.class || clazz == Integer.class);
    }
}
