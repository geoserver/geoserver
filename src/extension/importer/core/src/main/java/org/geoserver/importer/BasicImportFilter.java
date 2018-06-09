/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple filter used to constrain the tasks/items process during an import.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class BasicImportFilter implements ImportFilter {

    Set<ImportTask> set = new HashSet<ImportTask>();

    public void add(ImportTask task) {
        set.add(task);
    }

    public boolean include(ImportTask task) {
        return set.contains(task);
    }
}
