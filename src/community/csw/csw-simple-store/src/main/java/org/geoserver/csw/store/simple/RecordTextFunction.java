/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.capability.FunctionName;

/**
 * Concatenates the contents of all SimpleLiteral attributes into a single large string
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class RecordTextFunction extends FunctionExpressionImpl {

    public static FunctionName NAME = new FunctionNameImpl("cswRecordText");

    public RecordTextFunction() {
        super(NAME);
    }

    public Object evaluate(Object object) {
        Feature feature = (Feature) object;

        StringBuilder sb = new StringBuilder();
        for (Property p : feature.getProperties()) {
            if (p.getDescriptor().getType() == CSWRecordDescriptor.SIMPLE_LITERAL) {
                Object value = ((ComplexAttribute) p).getProperty("value").getValue();
                sb.append(value).append(" ");
            }
        }

        return sb.toString();
    }

}
