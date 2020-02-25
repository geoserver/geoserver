/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Dynamically loads the current list of coverage names from the catalog
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CoverageNamesModel extends LoadableDetachableModel<List<String>> {
    private static final long serialVersionUID = 6445323794739973799L;

    @Override
    protected List<String> load() {
        // get the list of coverages
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<CoverageInfo> coverages = catalog.getCoverages();

        // build the sorted list of names
        List<String> result = new ArrayList<String>();
        for (CoverageInfo ci : coverages) {
            result.add(ci.prefixedName());
        }
        Collections.sort(result);
        return result;
    }
}
