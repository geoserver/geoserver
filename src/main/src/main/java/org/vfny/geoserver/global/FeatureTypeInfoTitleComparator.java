/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.util.Comparator;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.geoserver.catalog.FeatureTypeInfo;

/**
 * A comparator used to sort {@link FeatureTypeInfo} objects by title 
 * (useful for capabilities output, for example)
 * @author Andrea Aime - TOPP
 */
public class FeatureTypeInfoTitleComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        FeatureTypeInfo ft1 = (FeatureTypeInfo) o1;
        FeatureTypeInfo ft2 = (FeatureTypeInfo) o2;
        
        // this will take care of null values as well
        return new CompareToBuilder().append(ft1.getTitle(), ft2.getTitle()).toComparison();
    }
    
}