/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac.functions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.Collections;
import java.util.List;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;

/**
 * Builds a URL based on a <code>String.format()</code> like template and a list of parameters to
 * embed into it
 */
public class ServiceLinkFunction extends FunctionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "serviceLink",
                    String.class,
                    parameter("template", String.class),
                    parameter("param", Object.class, 0, Integer.MAX_VALUE));

    public ServiceLinkFunction() {
        this.functionName = NAME;
    }

    @Override
    public Object evaluate(Object feature) {
        List<Expression> params = getParameters();
        String template = params.get(0).evaluate(feature, String.class);
        if (template == null) return null;

        Object[] templateParameters =
                params.stream()
                        .skip(1)
                        .map(p -> p.evaluate(feature, String.class))
                        .map(v -> v != null ? ResponseUtils.urlEncode(v) : null)
                        .toArray();

        String path = String.format(template, templateParameters);

        APIRequestInfo requestInfo = APIRequestInfo.get();
        if (requestInfo == null) return path; // just for testing purposes
        return ResponseUtils.buildURL(
                requestInfo.getBaseURL(), path, Collections.EMPTY_MAP, URLMangler.URLType.SERVICE);
    }
}
