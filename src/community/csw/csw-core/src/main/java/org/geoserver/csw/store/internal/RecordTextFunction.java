/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.csw.store.internal.CatalogStoreMapping.CatalogStoreMappingElement;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

/**
 * Collects the contents of all SimpleLiteral attributes into a collection
 * 
 * @author Niels Charlier
 */
public class RecordTextFunction extends FunctionExpressionImpl {

    public static FunctionName NAME = new FunctionNameImpl("cswRecordText");
    protected CatalogStoreMapping mapping;

    public RecordTextFunction(CatalogStoreMapping mapping) {
        super(NAME);
        this.mapping = mapping;
    }

    public Object evaluate(Object object) {
        
        List<Object> list = new ArrayList<Object>(mapping.elements().size());
        for (CatalogStoreMappingElement element : mapping.elements()) {
        	Object value = element.getContent().evaluate(object);
        	if (value != null) {
        		list.add( value );
        	}
        }

        return list;
    }

}
