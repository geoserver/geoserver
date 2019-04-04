/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import java.util.logging.Logger;
import net.opengis.wfs20.ResultTypeType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geotools.util.logging.Logging;

/**
 * When a request that contains the "resultType" parameter arrives, if the parameter value is
 * "index" it is substituted by "hits".
 *
 * <p>A new entry named RESULT_TYPE_INDEX specifying that the original result type was "index" is
 * added to KVP maps
 *
 * <p>The object that manage response of type HitsOutputFormat is replaced with IndexOutputFormat
 * before response has been dispatched
 *
 * @author sandr
 */
public class IndexResultTypeDispatcherCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(IndexResultTypeDispatcherCallback.class);

    private GeoServer gs;

    private IndexConfigurationManager indexConfiguration;

    private static final String RESULT_TYPE_PARAMETER = "resultType";

    private static final String RESULT_TYPE_INDEX = "index";

    static final String RESULT_TYPE_INDEX_PARAMETER = "RESULT_TYPE_INDEX";

    public IndexResultTypeDispatcherCallback(
            GeoServer gs, IndexConfigurationManager indexConfiguration) {
        this.gs = gs;
        this.indexConfiguration = indexConfiguration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Request init(Request request) {
        Object resultType = request.getKvp().get(RESULT_TYPE_PARAMETER);
        if (resultType != null && resultType.toString().equals(RESULT_TYPE_INDEX)) {
            request.getKvp().put(RESULT_TYPE_PARAMETER, ResultTypeType.HITS);
            request.getKvp().put(RESULT_TYPE_INDEX_PARAMETER, true);
        }
        return super.init(request);
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        Response newResponse = response;
        if (request.getKvp().get(RESULT_TYPE_INDEX_PARAMETER) != null
                && (Boolean) request.getKvp().get(RESULT_TYPE_INDEX_PARAMETER)) {
            IndexOutputFormat r = new IndexOutputFormat(this.gs, this.indexConfiguration);
            r.setRequest(request);
            newResponse = r;
        }
        return super.responseDispatched(request, operation, result, newResponse);
    }
}
