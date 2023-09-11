/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.capability.FunctionNameImpl;

/** Check if the current {@link Request} matches the regex passed as an argument of the Function. */
public class RequestMatchRegex extends RequestFunction {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "requestMatchRegex",
                    parameter("result", Boolean.class),
                    parameter("regex", String.class));

    public RequestMatchRegex() {
        super(NAME);
    }

    @Override
    protected Object evaluateInternal(Request request, Object object) {
        String regex = getParameters().get(0).evaluate(null, String.class);
        Pattern pattern = Pattern.compile(regex);
        String url = getFullURL(request.getHttpRequest());
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    private String getFullURL(HttpServletRequest request) {
        StringBuilder requestURL = new StringBuilder(ResponseUtils.baseURL(request));
        String pathInfo = request.getPathInfo();
        String queryString = request.getQueryString();
        if (pathInfo != null) {
            if (pathInfo.startsWith("/")) pathInfo = pathInfo.substring(1);
            requestURL.append(pathInfo);
        }
        if (queryString != null) {
            requestURL.append("?").append(queryString);
        }
        return requestURL.toString();
    }
}
