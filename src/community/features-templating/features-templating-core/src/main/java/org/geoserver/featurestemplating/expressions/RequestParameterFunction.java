/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.Request;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Converters;
import org.opengis.filter.capability.FunctionName;

/** Returns the value of the request parameter with the name specified in the function parameter. */
public class RequestParameterFunction extends RequestFunction {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "requestParam",
                    parameter("result", String.class),
                    parameter("name", String.class));

    public RequestParameterFunction() {
        super(NAME);
    }

    @Override
    protected Object evaluateInternal(Request request, Object object) {
        String parameter = getParameters().get(0).evaluate(null, String.class);
        HttpServletRequest req = request.getHttpRequest();
        Object value = null;
        if (req != null) {
            value = req.getParameter(parameter);
        }
        Map<String, Object> rawKvp = request.getRawKvp();
        if (rawKvp != null && value == null) value = request.getRawKvp().get(parameter);

        return Converters.convert(value, String.class);
    }
}
