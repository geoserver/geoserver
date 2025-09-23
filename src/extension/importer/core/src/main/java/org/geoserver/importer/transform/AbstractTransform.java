/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.Serial;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public abstract class AbstractTransform implements ImportTransform {

    @Serial
    private static final long serialVersionUID = 1L;

    final transient Logger LOGGER = Logging.getLogger(getClass());

    @Override
    public boolean stopOnError(Exception e) {
        return true;
    }

    /**
     * Make subclassing less onerous. If an implementation has temporary or transient state, this method allows a hook
     * to create that.
     */
    @Override
    public void init() {
        // do nothing
    }

    @Override
    public String toString() {
        // at least provide the name of the transform in logs, if the subclass did not override
        return getClass().getSimpleName();
    }
}
