/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.util.Date;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.feature.type.DateUtil;

abstract class AbstractCapabilitiesDefaultValueSelectionStrategy extends AbstractDefaultValueStrategy {
    /** serialVersionUID */
    private static final long serialVersionUID = -4316579372963159438L;

    @Override
    public String getCapabilitiesRepresentation(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        String retval = null;
        if (dimensionName.equals(ResourceInfo.TIME)){
            Date dateValue = getDefaultValue(resource, dimensionName, dimensionInfo, Date.class);
            retval = DateUtil.serializeDateTime(dateValue.getTime(), true);
        }
        else {
            Number numberValue = getDefaultValue(resource, dimensionName, dimensionInfo, Number.class);
            retval = numberValue.toString();
        }
        return retval;
    }
}