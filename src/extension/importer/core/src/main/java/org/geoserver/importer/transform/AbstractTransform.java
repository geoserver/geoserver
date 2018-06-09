/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public abstract class AbstractTransform implements ImportTransform {

    private static final long serialVersionUID = 1L;

    final transient Logger LOGGER = Logging.getLogger(getClass());

    public boolean stopOnError(Exception e) {
        return true;
    }

    /**
     * Make subclassing less onerous. If an implementation has temporary or transient state, this
     * method allows a hook to create that.
     */
    public void init() {
        // do nothing
    }
}
