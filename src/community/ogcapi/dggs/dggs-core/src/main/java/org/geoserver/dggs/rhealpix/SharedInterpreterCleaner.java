/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.dggs.rhealpix;

import java.util.logging.Level;
import java.util.logging.Logger;
import jep.JepException;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geotools.dggs.rhealpix.JEPWebRuntime;
import org.geotools.util.logging.Logging;

public class SharedInterpreterCleaner extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(SharedInterpreterCleaner.class);

    @Override
    public void finished(Request request) {
        try {
            JEPWebRuntime.closeThreadIntepreter();
        } catch (JepException e) {
            LOGGER.log(
                    Level.FINE,
                    "Exception happened while cleaning up JEP shared runtime for rHealPix DDGGS support",
                    e);
        }
    }
}
