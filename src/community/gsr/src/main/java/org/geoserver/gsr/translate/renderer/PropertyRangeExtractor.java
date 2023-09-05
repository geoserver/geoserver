/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import org.geotools.api.filter.Filter;

public interface PropertyRangeExtractor {

    /**
     * Returns a range representation from the filter, or null if the filter could not be converted
     * to an equivalent range
     *
     * @param filter
     * @return
     */
    PropertyRange getRange(Filter filter);
}
