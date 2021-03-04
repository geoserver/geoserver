/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.util.Comparator;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.geoserver.catalog.FeatureTypeInfo;

/**
 * A comparator used to sort {@link FeatureTypeInfo} objects by title (useful for capabilities
 * output, for example)
 *
 * @author Andrea Aime - TOPP
 */
public class FeatureTypeInfoTitleComparator implements Comparator<FeatureTypeInfo> {

    @Override
    public int compare(FeatureTypeInfo ft1, FeatureTypeInfo ft2) {
        // this will take care of null values as well
        return new CompareToBuilder().append(ft1.getTitle(), ft2.getTitle()).toComparison();
    }
}
