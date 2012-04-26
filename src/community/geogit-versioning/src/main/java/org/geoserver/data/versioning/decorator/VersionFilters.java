package org.geoserver.data.versioning.decorator;

import java.util.HashSet;
import java.util.Set;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.identity.ResourceIdImpl;
import org.geotools.filter.visitor.AbstractFinderFilterVisitor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.identity.ResourceId;
import org.opengis.filter.identity.Version;

public class VersionFilters {

    /**
     * @return a copy of {@code filter} with any {@link ResourceId} stripped off and converted to
     *         normal {@link FeatureId} with no version information.
     */
    public static final Filter getUnversioningFilter(Filter filter) {
        if (filter == null || Filter.INCLUDE.equals(filter) || Filter.EXCLUDE.equals(filter)) {
            return filter;
        }
        DuplicatingFilterVisitor visitor = new DuplicatingFilterVisitor() {
            @Override
            public Object visit(final Id filter, final Object data) {
                Set<Identifier> featureIds = new HashSet<Identifier>();
                for (Identifier id : filter.getIdentifiers()) {
                    if (id instanceof FeatureId && !(id instanceof ResourceId)) {
                        String rid = ((FeatureId) id).getID();
                        int idx = rid.indexOf(ResourceId.VERSION_SEPARATOR);
                        if (idx > 0) {
                            String fid = rid.substring(0, idx);
                            featureIds.add(getFactory(data).featureId(fid));
                        } else {
                            featureIds.add(id);
                        }
                    } else {
                        featureIds.add(id);
                    }
                }
                return getFactory(data).id(featureIds);
            }
        };

        return (Filter) filter.accept(visitor, null);
    }

    public static final Id getVersioningFilter(Filter filter) {

        if (filter == null || Filter.INCLUDE.equals(filter) || Filter.EXCLUDE.equals(filter)) {
            return null;
        }

        final FilterVisitor ridFinder = new AbstractFinderFilterVisitor() {
            @Override
            public Object visit(final Id filter, final Object data) {
                Set<ResourceId> resourceIds = new HashSet<ResourceId>();
                for (Identifier id : filter.getIdentifiers()) {
                    if (id instanceof ResourceId) {
                        // does it contain any actual versioning predicate?
                        ResourceId rid = (ResourceId) id;
                        if (rid.getFeatureVersion() != null || rid.getEndTime() != null
                                || rid.getStartTime() != null || rid.getVersion() != null) {
                            // yes, there's something to query in the version history
                            resourceIds.add((ResourceId) id);
                        }
                    } else if (id instanceof FeatureId) {
                        FeatureId fid = (FeatureId) id;
                        int idx = fid.getID().indexOf(FeatureId.VERSION_SEPARATOR);
                        if (idx > 0) {
                            String featureId = fid.getID().substring(0, idx);
                            String versionId = fid.getID().substring(idx + 1);
                            resourceIds.add(new ResourceIdImpl(featureId, versionId));
                        }
                    }
                }
                if (resourceIds.size() > 0) {
                    found = true;
                    return CommonFactoryFinder.getFilterFactory2(null).id(resourceIds);
                }
                return null;
            }
        };

        Object found = filter.accept(ridFinder, null);
        if (found instanceof Id) {
            return (Id) found;
        }

        return null;
    }

}
