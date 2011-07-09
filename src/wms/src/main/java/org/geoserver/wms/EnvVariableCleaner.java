/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geotools.filter.function.EnvFunction;

/**
 * Cleans up the {@link EnvFunction} local values once the request is complete, makes sure the
 * filters have had the largest possible span to evaluate environment variables (will work both
 * during image generation but also for streaming encoding)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class EnvVariableCleaner extends AbstractDispatcherCallback {

    @Override
    public void finished(Request request) {
        EnvFunction.clearLocalValues();
    }

}
