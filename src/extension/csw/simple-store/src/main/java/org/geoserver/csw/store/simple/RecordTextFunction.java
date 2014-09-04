/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.capability.FunctionName;

/**
 * Collects the contents of all SimpleLiteral attributes into a collection
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

        List<Object> list = new ArrayList<Object>(feature.getProperties().size());
        for (Property p : feature.getProperties()) {
            if (p.getDescriptor().getType() == CSWRecordDescriptor.SIMPLE_LITERAL) {
                Object value = ((ComplexAttribute) p).getProperty("value").getValue();
                list.add(value);
            }
        }

        return list;
    }

}
