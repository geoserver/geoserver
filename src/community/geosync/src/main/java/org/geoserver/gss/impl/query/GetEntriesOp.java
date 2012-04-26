/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.query;

import java.util.List;

import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.gss.service.FeedType;
import org.geoserver.gss.service.GetEntries;
import org.geoserver.platform.ServiceException;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

public class GetEntriesOp {

    private final GSS gss;

    public GetEntriesOp(GSS gss) {
        this.gss = gss;
    }

    /**
     * 
     * @precondition {@code request != null}
     * @precondition {@code request.getFeed() != null}
     * @precondition {@code request.getOutputFormat() != null}
     * @precondition {@code request.getFilter() != null}
     * @precondition {@code request.getStartPosition() >= 0}
     * @precondition {@code request.getMaxEntries >= 0}
     * 
     * @param request
     * @return
     * @throws ServiceException
     */
    public FeedImpl execute(final GetEntries request) throws ServiceException {

        // final String baseUrl = request.getBaseUrl();
        // final String handle = request.getHandle();

        final FeedType feed = request.getFeed();
        final Long startPosition = request.getStartPosition();
        final Long maxEntries = request.getMaxEntries();

        final Filter filter = request.getFilter();
        final List<String> searchTerms = request.getSearchTerms();

        final SortOrder sortOrder = request.getSortOrder() == null ? SortOrder.ASCENDING : request
                .getSortOrder();

        FeedImpl response;

        switch (feed) {
        case CHANGEFEED:
            response = gss.queryChangeFeed(searchTerms, filter, startPosition, maxEntries,
                    sortOrder);
            break;
        case RESOLUTIONFEED:
            response = gss.queryResolutionFeed(searchTerms, filter, startPosition, maxEntries,
                    sortOrder);
            break;
        case REPLICATIONFEED:
            response = gss.queryReplicationFeed(searchTerms, filter, startPosition, maxEntries,
                    sortOrder);
            break;
        default:
            throw new IllegalArgumentException("Unknown feed type: " + feed);
        }

        return response;
    }

}
