package org.geoserver.gss.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geotools.util.Range;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

/**
 * Builds a feed of entries for the RESOLUTION FEED based on the given filtering criteria.
 * 
 * @author groldan
 * 
 */
class CommitsEntryListBuilder {

    private final GeoGIT geoGit;

    private List<String> searchTerms;

    /**
     * A Filter against the {@link EntryImpl} construct
     */
    private Filter filter;

    private Long startPosition;

    private Long maxEntries;

    private final GSS gss;

    private SortOrder sortOrder;

    public CommitsEntryListBuilder(GSS gss, GeoGIT geoGit) {
        this.gss = gss;
        this.geoGit = geoGit;
        this.startPosition = 1L;
        this.maxEntries = 25L;
        this.sortOrder = SortOrder.ASCENDING;
        this.filter = Filter.INCLUDE;
    }

    public void setSearchTerms(List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public void setStartPosition(Long startPosition) {
        this.startPosition = startPosition;
    }

    public void setMaxEntries(Long maxEntries) {
        this.maxEntries = maxEntries;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Performs a ggit log operation that satisfies the time constraints and with the obtained list
     * of commits, geenerates a feed with an entry per feature change in every commit.
     * <p>
     * The non time related constraints are evaluated in-process by the resulting list of entries.
     * </p>
     * 
     * @return
     * @throws Exception
     */
    public FeedImpl buildFeed() throws Exception {
        final Filter filter = this.filter == null ? Filter.INCLUDE : this.filter;
        final TimeConstraintExtractor timeConstraintExtractor = new TimeConstraintExtractor();
        filter.accept(timeConstraintExtractor, null);
        final Range<Date> commitRange = timeConstraintExtractor.getValidTimeWindow();

        Iterator<RevCommit> commits = geoGit.log().setTimeRange(commitRange).call();
        if (SortOrder.ASCENDING.equals(this.sortOrder)) {
            LinkedList<RevCommit> list = new LinkedList<RevCommit>();
            while (commits.hasNext()) {
                list.addFirst(commits.next());
            }
            commits = list.iterator();
        }

        final Function<RevCommit, EntryImpl> commitToEntryFunctor = new CommitToEntry(geoGit);
        Iterator<EntryImpl> entries = Iterators.transform(commits, commitToEntryFunctor);

        FilteringEntryListBuilder filteringEntries = new FilteringEntryListBuilder(this.filter,
                this.searchTerms, this.startPosition, this.maxEntries);

        entries = filteringEntries.filter(entries);

        FeedImpl feed = buildFeedImpl(entries);

        return feed;

    }

    private FeedImpl buildFeedImpl(final Iterator<EntryImpl> entries) {
        FeedImpl feed = new FeedImpl();
        feed.setStartPosition(startPosition);
        feed.setMaxEntries(maxEntries);

        final Ref head = geoGit.getRepository().getRef(Ref.HEAD);
        if (head.getObjectId().isNull()) {
            // ah, so there's still not even a single commit in the whole database
            feed.setId(FeedImpl.NULL_ID);
            feed.setUpdated(new Date());
        } else {
            RevCommit headCommit = geoGit.getRepository().getCommit(head.getObjectId());
            feed.setId(headCommit.getId().toString());
            feed.setUpdated(new Date(headCommit.getTimestamp()));
        }
        feed.setEntry(entries);
        return feed;
    }

}
