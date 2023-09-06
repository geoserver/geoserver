/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import java.util.logging.Logger;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.util.logging.Logging;

/**
 * Abstract function that evaluate against a {@link Request} object. Subclasses must implement
 * evaluate internal.
 */
public abstract class RequestFunction extends FunctionExpressionImpl {

    private static final Logger LOGGER = Logging.getLogger(RequestFunction.class);

    protected RequestFunction(FunctionName functionName) {
        super(functionName);
    }

    @Override
    public Object evaluate(Object object) {
        Request request;
        if (object != null && object instanceof Request) request = (Request) object;
        else request = Dispatcher.REQUEST.get();
        if (request == null) {
            LOGGER.info("Found a null Request object. Returning null");
            return null;
        } else return evaluateInternal(request, object);
    }

    protected abstract Object evaluateInternal(Request request, Object object);
}
