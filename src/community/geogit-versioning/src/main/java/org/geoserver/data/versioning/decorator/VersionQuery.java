package org.geoserver.data.versioning.decorator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffOp;
import org.geogit.api.GeoGIT;
import org.geogit.api.LogOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.repository.Repository;
import org.geogit.storage.StagingDatabase;
import org.geotools.util.Range;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.ResourceId;
import org.opengis.filter.identity.Version;
import org.opengis.filter.identity.Version.Action;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

public class VersionQuery {

    private final Name typeName;

    private final GeoGIT ggit;

    public VersionQuery(final GeoGIT ggit, final Name typeName) {
        this.ggit = ggit;
        this.typeName = typeName;
    }

    /**
     * @param id
     * @return an iterator for all the requested versions of a given feature, or the empty iterator
     *         if no such feature is found.
     * @throws Exception
     */
    public Iterator<Ref> get(final ResourceId id) throws Exception {
        final String featureId = id.getID();
        final String featureVersion = id.getFeatureVersion();

        final Version version = id.getVersion();
        final boolean isDateRangeQuery = id.getStartTime() != null || id.getEndTime() != null;
        final boolean isVesionQuery = !version.isEmpty();

        final Ref requestedVersionRef = extractRequestedVersion(ggit, featureId, featureVersion);
        {
            final boolean explicitVersionQuery = !isDateRangeQuery && !isVesionQuery;
            if (explicitVersionQuery) {
                if (requestedVersionRef == null) {
                    return Iterators.emptyIterator();
                } else {
                    // easy, no extra constraints specified
                    return Iterators.singletonIterator(requestedVersionRef);
                }
            }
        }

        // at this point is either a version query or a date range query...

        List<Ref> result = new ArrayList<Ref>(5);

        // filter commits that affect the requested feature
        final List<String> path = path(featureId);
        LogOp logOp = ggit.log().addPath(path);

        if (isDateRangeQuery) {
            // time range query, limit commits by time range, if speficied
            Date startTime = id.getStartTime() == null ? new Date(0L) : id.getStartTime();
            Date endTime = id.getEndTime() == null ? new Date(Long.MAX_VALUE) : id.getEndTime();
            boolean isMinIncluded = true;
            boolean isMaxIncluded = true;
            Range<Date> timeRange = new Range<Date>(Date.class, startTime, isMinIncluded, endTime,
                    isMaxIncluded);
            logOp.setTimeRange(timeRange);
        }

        // all commits whose tree contains the requested feature
        Iterator<RevCommit> featureCommits = logOp.call();

        if (isDateRangeQuery) {
            List<Ref> allInAscendingOrder = getAllInAscendingOrder(ggit, featureCommits, featureId);
            result.addAll(allInAscendingOrder);
        } else if (isVesionQuery) {
            if (version.isDateTime()) {
                final Date validAsOf = version.getDateTime();
                RevCommit closest = findClosest(validAsOf, featureCommits);
                if (closest != null) {
                    featureCommits = Iterators.singletonIterator(closest);
                    result.addAll(getAllInAscendingOrder(ggit, featureCommits, featureId));
                }
            } else if (version.isIndex()) {
                final int requestIndex = version.getIndex().intValue();
                if (0 >= requestIndex) {
                    throw new IllegalArgumentException(
                            "Invalid ResourceId version index requested: " + requestIndex
                                    + ". Must be a positive integer > 0.");
                }
                final int listIndex = requestIndex - 1;// version indexing starts at 1
                List<Ref> allVersions = getAllInAscendingOrder(ggit, featureCommits, featureId);
                if (allVersions.size() > 0) {
                    if (allVersions.size() >= requestIndex) {
                        result.add(allVersions.get(listIndex));
                    } else {
                        result.add(allVersions.get(allVersions.size() - 1));
                    }
                }
            } else if (version.isVersionAction()) {
                final Action versionAction = version.getVersionAction();
                List<Ref> allInAscendingOrder = getAllInAscendingOrder(ggit, featureCommits,
                        featureId);
                switch (versionAction) {
                case ALL:
                    result.addAll(allInAscendingOrder);
                    break;
                case FIRST:
                    if (allInAscendingOrder.size() > 0) {
                        result.add(allInAscendingOrder.get(0));
                    }
                    break;
                case LAST:
                    if (allInAscendingOrder.size() > 0) {
                        result.add(allInAscendingOrder.get(allInAscendingOrder.size() - 1));
                    }
                    break;
                case NEXT:
                    Ref next = next(requestedVersionRef, allInAscendingOrder);
                    if (next != null) {
                        result.add(next);
                    }
                    break;
                case PREVIOUS:
                    Ref previous = previous(requestedVersionRef, allInAscendingOrder);
                    if (previous != null) {
                        result.add(previous);
                    }
                    break;
                default:
                    break;
                }
            }
        }
        return result.iterator();
    }

    private RevCommit findClosest(final Date date, Iterator<RevCommit> commitsInDescendingOrder) {
        final long requestedTime = date.getTime();
        RevCommit closest = null;
        while (commitsInDescendingOrder.hasNext()) {
            RevCommit current = commitsInDescendingOrder.next();
            if (closest == null) {
                closest = current;
            } else {
                long delta = Math.abs(current.getTimestamp() - requestedTime);
                long prevDelta = Math.abs(closest.getTimestamp() - requestedTime);
                if (delta < prevDelta) {
                    closest = current;
                }
            }
        }
        return closest;
    }

    private long toSecondsPrecision(final long timeStampMillis) {
        return timeStampMillis / 1000;
    }

    private Ref previous(Ref requestedVersionRef, List<Ref> allVersions) {
        int idx = locate(requestedVersionRef, allVersions);
        if (idx > 0) {
            return allVersions.get(idx - 1);
        }
        return null;
    }

    private Ref next(Ref requestedVersionRef, List<Ref> allVersions) {
        int idx = locate(requestedVersionRef, allVersions);
        if (idx > -1 && idx < allVersions.size() - 1) {
            return allVersions.get(idx + 1);
        }
        return null;
    }

    private int locate(final Ref requestedVersionRef, List<Ref> allVersions) {
        if (requestedVersionRef == null) {
            return -1;
        }
        for (int i = 0; i < allVersions.size(); i++) {
            Ref ref = allVersions.get(i);
            if (requestedVersionRef.equals(ref)) {
                return i;
            }
        }
        return -1;
    }

    private List<Ref> getAllInAscendingOrder(final GeoGIT ggit, final Iterator<RevCommit> commits,
            final String featureId) throws Exception {

        LinkedList<Ref> featureRefs = new LinkedList<Ref>();

        final List<String> path = path(featureId);
        // find all commits where this feature is touched
        while (commits.hasNext()) {
            RevCommit commit = commits.next();
            ObjectId commitId = commit.getId();
            ObjectId parentCommitId = commit.getParentIds().get(0);
            DiffOp diffOp = ggit.diff().setOldVersion(parentCommitId).setNewVersion(commitId)
                    .setFilter(path);
            Iterator<DiffEntry> diffs = diffOp.call();
            Preconditions.checkState(diffs.hasNext());
            DiffEntry diff = diffs.next();
            Preconditions.checkState(!diffs.hasNext());
            switch (diff.getType()) {
            case ADD:
            case MODIFY:
                featureRefs.addFirst(diff.getNewObject());
                break;
            case DELETE:
                break;
            }
        }
        return featureRefs;
    }

    /**
     * Extracts the feature version from the given {@code rid} if supplied, or finds out the current
     * feature version from the feature id otherwise.
     * 
     * @return the version identifier of the feature given by {@code version}, or at the current
     *         geogit HEAD if {@code version == null}, or {@code null} if such a feature does not
     *         exist.
     */
    private Ref extractRequestedVersion(final GeoGIT ggit, final String featureId,
            final String version) {
        final Repository repository = ggit.getRepository();
        if (version != null) {
            ObjectId versionedId = ObjectId.valueOf(version);
            // verify the object exists
            StagingDatabase stagingDatabase = repository.getIndex().getDatabase();
            boolean exists = stagingDatabase.exists(versionedId);
            // Ref rootTreeChild = repository.getRootTreeChild(path(featureId));
            if (exists) {
                return new Ref(featureId, versionedId, TYPE.BLOB);
            }
            return null;
        }
        // no version specified, find out the latest
        List<String> path = path(featureId);
        Ref currFeatureRef = repository.getRootTreeChild(path);
        if (currFeatureRef == null) {
            // feature does not exist at the current repository state
            return null;
        }
        return currFeatureRef;
    }

    private List<String> path(final String featureId) {
        List<String> path = new ArrayList<String>(3);

        if (null != typeName.getNamespaceURI()) {
            path.add(typeName.getNamespaceURI());
        }
        path.add(typeName.getLocalPart());
        path.add(featureId);

        return path;
    }

}
