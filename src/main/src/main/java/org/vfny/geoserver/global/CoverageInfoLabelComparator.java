/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.util.Comparator;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.geoserver.catalog.CoverageInfo;

/**
 * A comparator used to sort {@link CoverageInfo} objects by label 
 * (useful for capabilities output, for example)
 * @author Andrea Aime - TOPP
 */
public class CoverageInfoLabelComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        CoverageInfo c1 = (CoverageInfo) o1;
        CoverageInfo c2 = (CoverageInfo) o2;
        
        // this will take care of null values as well
        return new CompareToBuilder().append(c1.getTitle(), c2.getTitle()).toComparison();
    }

}
