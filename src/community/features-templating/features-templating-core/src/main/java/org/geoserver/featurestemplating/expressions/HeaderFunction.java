package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geoserver.ows.Request;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Converters;

/** Returns the value of request header with the name specified in the parameter. */
public class HeaderFunction extends RequestFunction {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "header", parameter("result", String.class), parameter("name", String.class));

    public HeaderFunction() {
        super(NAME);
    }

    @Override
    protected Object evaluateInternal(Request request, Object object) {
        String parameter = getParameters().get(0).evaluate(null, String.class);
        Object value = request.getHttpRequest().getHeader(parameter);
        return Converters.convert(value, String.class);
    }
}
