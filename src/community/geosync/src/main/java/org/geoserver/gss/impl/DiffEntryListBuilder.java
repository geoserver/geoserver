package org.geoserver.gss.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffOp;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.repository.Repository;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geotools.util.Range;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.sort.SortOrder;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

/**
 * Builds a feed result for the REPLICATIONFEED based on the given filtering criteria.
 * 
 * @author groldan
 * 
 */
class DiffEntryListBuilder {

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

    public DiffEntryListBuilder(GSS gss, GeoGIT geoGit) {
        this.gss = gss;
        this.geoGit = geoGit;
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

        final Iterator<DiffEntry> diffEntries;

        if (filter instanceof Id) {
            diffEntries = getEntriesById((Id) filter);
        } else {
            LinkedList<Iterator<DiffEntry>> iterators = getEntries(filter);
            @SuppressWarnings("unchecked")
            final Iterator<DiffEntry>[] array = iterators.toArray(new Iterator[iterators.size()]);
            diffEntries = Iterators.concat(array);
        }

        Function<DiffEntry, EntryImpl> diffToEntryFunction = new DiffToEntry(gss);

        Iterator<EntryImpl> entries = Iterators.transform(diffEntries, diffToEntryFunction);

        FilteringEntryListBuilder filteringEntries = new FilteringEntryListBuilder(this.filter,
                this.searchTerms, this.startPosition, this.maxEntries);

        entries = filteringEntries.filter(entries);

        FeedImpl feed = buildFeed(entries);

        return feed;
    }

    private LinkedList<Iterator<DiffEntry>> getEntries(Filter filter) throws Exception {

        final TimeConstraintExtractor timeConstraintExtractor = new TimeConstraintExtractor();
        filter.accept(timeConstraintExtractor, null);
        final Range<Date> commitRange = timeConstraintExtractor.getValidTimeWindow();

        final Iterator<RevCommit> commits = geoGit.log().setTimeRange(commitRange).call();

        LinkedList<Iterator<DiffEntry>> iterators = new LinkedList<Iterator<DiffEntry>>();
        // needed for the top level feed metadata
        RevCommit newest = null;
        while (commits.hasNext()) {
            RevCommit commit = commits.next();

            if (newest == null) {
                newest = commit;
            }

            final ObjectId directParentId = commit.getParentIds().get(0);
            final ObjectId commitId = commit.getId();
            DiffOp diff = geoGit.diff().setOldVersion(directParentId).setNewVersion(commitId);

            final Iterator<DiffEntry> commitChanges = diff.call();

            final SortOrder order = sortOrder == null ? SortOrder.ASCENDING : sortOrder;
            if (SortOrder.ASCENDING.equals(order)) {
                /*
                 * Use adFirst so that the changes get sorted oldest first
                 */
                iterators.addFirst(commitChanges);
            } else {
                iterators.addLast(commitChanges);
            }
        }
        return iterators;
    }

    private Iterator<DiffEntry> getEntriesById(Id filter) throws Exception {
        final Repository repository = geoGit.getRepository();

        List<DiffEntry> entries = new LinkedList<DiffEntry>();

        for (Identifier id : filter.getIdentifiers()) {
            final String uuid = String.valueOf(id.getID());
            final String oldCommitHash = UUIDUtil.getOldCommitHash(uuid);
            final String newCommitHash = UUIDUtil.getNewCommitHash(uuid);
            final String blobHash = UUIDUtil.getBlobHash(uuid);

            ObjectId fromCommit;
            ObjectId toCommit;
            try {
                RevCommit oldCommit = repository.resolve(oldCommitHash, RevCommit.class);
                fromCommit = oldCommit.getId();
            } catch (NoSuchElementException e) {
                fromCommit = ObjectId.NULL;
            }
            try {
                RevCommit newCommit = repository.resolve(newCommitHash, RevCommit.class);
                toCommit = newCommit.getId();
            } catch (NoSuchElementException e) {
                toCommit = ObjectId.NULL;
            }

            RevObject blob = repository.resolve(blobHash);

            DiffOp diff = geoGit.diff().setOldVersion(fromCommit).setNewVersion(toCommit);
            diff.setFilter(blob.getId());

            Iterator<DiffEntry> iterator = diff.call();
            Preconditions.checkState(iterator.hasNext());
            DiffEntry entry = iterator.next();
            entries.add(entry);
        }
        return entries.iterator();
    }

    private FeedImpl buildFeed(final Iterator<EntryImpl> entries) {
        FeedImpl feed = new FeedImpl();
        feed.setStartPosition(startPosition);
        feed.setMaxEntries(maxEntries);

        feed.setEntry(entries);

        return feed;
    }

}
