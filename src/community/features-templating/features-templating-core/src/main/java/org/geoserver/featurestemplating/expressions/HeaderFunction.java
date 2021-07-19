package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geoserver.ows.Request;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Converters;
import org.opengis.filter.capability.FunctionName;

/** Returns the value of request header with the name specified in the parameter. */
public class HeaderFunction extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "header", parameter("result", String.class), parameter("name", String.class));

    public HeaderFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        if (!(object instanceof Request)) {
            throw new UnsupportedOperationException(
                    NAME.getName() + " function works with request object only");
        }
        Request request = (Request) object;
        String parameter = getParameters().get(0).evaluate(null, String.class);
        Object value = request.getHttpRequest().getHeader(parameter);
        return Converters.convert(value, String.class);
    }
}
