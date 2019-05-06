/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.List;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityUtils;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.kvp.GetExecutionsKvpFilterBuilder;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Retrieves the list of available executions from the ProcessStore accordingly to the request
 * parameters.
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class Executions {

    public static final String NO_SUCH_PARAMETER_CODE = "NoSuchParameter";

    public static final String NO_SUCH_PROCESS_CODE = "NoSuchProcess";

    public static final String INTERNAL_SERVER_ERROR_CODE = "InternalServerError";

    private static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    static final Logger LOGGER = Logging.getLogger(Executions.class);

    private static final int MAX_EXECUTIONS_PER_PAGE = 10;

    private GeoServer gs;

    /** The object tracking the status of various processes */
    private ProcessStatusTracker statusTracker;

    /** The resource tracker, we use it to build the responses */
    private WPSResourceManager resources;

    /** Used by the response builder */
    private ApplicationContext ctx;

    public Executions(
            GeoServer gs,
            ProcessStatusTracker statusTracker,
            WPSResourceManager resources,
            ApplicationContext ctx) {
        this.gs = gs;
        this.statusTracker = statusTracker;
        this.resources = resources;
        this.ctx = ctx;
    }

    public Object run(GetExecutionsType request) {
        // Check whether the user is authenticated or not and, in the second case, if it is an
        // Administrator or not
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final Object principal =
                auth != null && auth.getPrincipal() != null ? auth.getPrincipal() : null;
        boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole(auth);
        if (!isAdmin) {
            // Anonymous users cannot access the list of executions at all
            if (principal == null) {
                throw new WPSException(
                        Executions.NO_SUCH_PROCESS_CODE, "No Process Execution available.");
            }
            // Non-admins are not allowed to fetch executions from other users
            else if (request.owner != null
                    && !request.owner.isEmpty()
                    && !SecurityUtils.getUsername(principal).equalsIgnoreCase(request.owner)) {
                throw new WPSException(
                        Executions.NO_SUCH_PARAMETER_CODE, "Invalid parameter 'owner' specified.");
            }
        }

        // Let's build the filter accordingly to the request parameters...
        GetExecutionsKvpFilterBuilder builder = new GetExecutionsKvpFilterBuilder(ff);

        // Filter by the owner of the Process (if applicable)
        if (request.owner != null && !request.owner.isEmpty()) {
            // you requested only the processes belonging to a certain user (and you have rights to
            // fetch them)
            builder.appendUserNameFilter(request.owner);
        } else if (!isAdmin) {
            // not an admin? The list should be filtered to your own processes
            builder.appendUserNameFilter(SecurityUtils.getUsername(principal));
        } // Otherwise you are an admin asking for all the processes

        // Filter by the Process Name (Identifier)
        if (request.identifier != null && !request.identifier.isEmpty()) {
            builder.appendProcessNameFilter(request.identifier);
        }

        // Filter by the STATUS of the Process
        if (request.status != null && !request.status.isEmpty()) {
            builder.appendStatusFilter(request.status);
        }

        Query queryFilter = new Query("GetExecutions", builder.getFilter());
        int total = statusTracker.getStore().list(queryFilter).size();

        // Now let's check the ordering and act accordingly
        if (request.orderBy != null && !request.orderBy.isEmpty()) {
            String sortAttribute = translateAttributeName(request.orderBy);
            if (sortAttribute != null && !sortAttribute.isEmpty()) {
                SortBy[] sortBy = new SortBy[] {ff.sort(sortAttribute, SortOrder.DESCENDING)};
                queryFilter.setSortBy(sortBy);
            }
        }

        // Set Pagination accordingly to the GSIP-169:
        // - if number less or equal than MAX_FEATURES_PER_PAGE, then go ahead
        // - if number greater than MAX_FEATURES_PER_PAGE
        // -- add "count" attribute to the GetExecutionsResponse,
        //    representing the total number of elements
        // -- add "next" attribute to the GetExecutionsResponse,
        //    representing the URL of the next page; it this is not present
        //    then there are no more pages available
        // -- add "previous" attribute to the GetExecutionsResponse,
        //    representing the URL of the previous page; it this is not present
        //    then we are at the first page
        Integer startIndex = request.getStartIndex();
        if (startIndex != null) {
            queryFilter.setStartIndex(startIndex);
        } else if (total > MAX_EXECUTIONS_PER_PAGE) {
            startIndex = 0;
        }

        Integer maxFeatures = request.getMaxFeatures();
        if (maxFeatures == null && total > MAX_EXECUTIONS_PER_PAGE) {
            maxFeatures = MAX_EXECUTIONS_PER_PAGE;
        }
        if (maxFeatures != null) {
            queryFilter.setMaxFeatures(maxFeatures);
        }

        List<ExecutionStatus> statuses = statusTracker.getStore().list(queryFilter);
        if (statuses == null) {
            throw new WPSException(
                    Executions.NO_SUCH_PROCESS_CODE, "No Process Execution available.");
        }

        // Going to collect all the responses outputs
        GetExecutionsTransformer executionsTransformer =
                new GetExecutionsTransformer(
                        gs.getService(WPSInfo.class),
                        resources,
                        ctx,
                        request,
                        total,
                        startIndex,
                        maxFeatures);
        if (!statuses.isEmpty()) {
            for (ExecutionStatus status : statuses) {
                executionsTransformer.append(status);
            }

            return executionsTransformer;
        }
        throw new WPSException(Executions.NO_SUCH_PROCESS_CODE, "No Process Execution available.");
    }

    private String translateAttributeName(String orderBy) {
        switch (orderBy.toLowerCase()) {
            case "owner":
                return "userName";
            case "identifier":
                return "processName";
            case "jobid":
                return "executionId";
            case "status":
                return "phase";
        }
        return null;
    }

    /** @return */
    private GeoServerSecurityManager getSecurityManager() {
        return ctx.getBean(GeoServerSecurityManager.class);
    }
}
