/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.ows.URLMangler.URLType.*;
import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.Collections;
import java.util.List;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;

/** Exposes QuickTemplate link building as an OGC function */
public class TemplateLinkFunction extends FunctionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl("templateLink", String.class, parameter("template", String.class));

    public TemplateLinkFunction() {
        this.functionName = NAME;
    }

    @Override
    public Object evaluate(Object feature) {
        List<Expression> params = getParameters();
        String template = params.get(0).evaluate(feature, String.class);
        if (template == null) return null;

        Request request = Dispatcher.REQUEST.get();
        if (request == null) return template; // just for testing purposes
        String baseURL = getHRefBase(request);
        return QuickTemplate.replaceVariables(
                template, Collections.singletonMap("${BASE_URL}", baseURL));
    }

    private String getHRefBase(Request request) {
        String baseURL = ResponseUtils.baseURL(request.getHttpRequest());
        String hrefBase = ResponseUtils.buildURL(baseURL, null, null, SERVICE);
        if (hrefBase.endsWith("/")) {
            hrefBase = hrefBase.substring(0, hrefBase.length() - 1);
        }
        return hrefBase;
    }
}
