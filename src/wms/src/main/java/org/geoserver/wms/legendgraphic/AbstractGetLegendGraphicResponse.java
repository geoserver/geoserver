/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.util.Set;
import java.util.TreeSet;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wms.GetLegendGraphicRequest;

abstract class AbstractGetLegendGraphicResponse extends Response {

    @SuppressWarnings("rawtypes")
    public AbstractGetLegendGraphicResponse(final Class binding, final String outputFormat) {
        super(binding, caseInsensitive(outputFormat));
    }

    private static Set<String> caseInsensitive(String outputFormat) {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.add(outputFormat);
        return set;
    }

    /** @see org.geoserver.ows.Response#canHandle(org.geoserver.platform.Operation) */
    @Override
    public boolean canHandle(Operation operation) {
        Object[] parameters = operation.getParameters();
        GetLegendGraphicRequest request =
                OwsUtils.parameter(parameters, GetLegendGraphicRequest.class);
        return request != null && getOutputFormats().contains(request.getFormat());
    }
}
