/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import static org.junit.Assert.assertEquals;

import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

public class CSVTestStrategySupport {

    public static String buildInputString(String... rows) {
        StringBuilder builder = new StringBuilder();
        for (String row : rows) {
            builder.append(row);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    public static void verifyType(AttributeDescriptor attributeDescriptor, Class<?> clazz) {
        AttributeType type = attributeDescriptor.getType();
        assertEquals("Unexpected type", clazz, type.getBinding());
    }

}
