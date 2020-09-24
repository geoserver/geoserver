/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.dggs;

import java.util.logging.Level;
import java.util.logging.Logger;
import jep.JepException;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.geoserver.web.WicketCallback;
import org.geotools.dggs.rhealpix.JEPWebRuntime;
import org.geotools.util.logging.Logging;

public class SharedInterpreterUICleaner implements WicketCallback {
    static final Logger LOGGER = Logging.getLogger(SharedInterpreterUICleaner.class);

    @Override
    public void onBeginRequest() {
        // nothing to do
    }

    @Override
    public void onAfterTargetsDetached() {
        // nothing to do
    }

    @Override
    public void onEndRequest() {
        try {
            JEPWebRuntime.closeThreadIntepreter();
        } catch (JepException e) {
            LOGGER.log(
                    Level.FINE,
                    "Exception happened while cleaning up JEP shared runtime for rHealPix DDGGS support",
                    e);
        }
    }

    @Override
    public void onRequestTargetSet(
            RequestCycle cycle, Class<? extends IRequestablePage> requestTarget) {
        // nothing to do
    }

    @Override
    public void onRuntimeException(RequestCycle cycle, Exception ex) {
        // nothing to do
    }
}
