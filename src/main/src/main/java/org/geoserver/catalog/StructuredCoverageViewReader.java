/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.feature.CompositeFeatureCollection;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;

/**
 * A coverageView reader using a structured coverage readers implementation
 *
 * @author Daniele Romagnoli - GeoSolutions
 */
public class StructuredCoverageViewReader extends CoverageViewReader
        implements StructuredGridCoverage2DReader {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(StructuredCoverageViewReader.class);

    /**
     * Pass this hint to {@link #getGranules(String, boolean)} in order to get back information from
     * a single band of the coverage view
     */
    public static Hints.Key QUERY_FIRST_BAND = new Hints.Key(Boolean.class);

    /** Unmaps the view prefix in the id names */
    static class GranuleStoreViewFilterVisitor extends DuplicatingFilterVisitor {

        static Filter unmapIdentifiers(Filter filter, String viewName) {
            GranuleStoreViewFilterVisitor visitor = new GranuleStoreViewFilterVisitor(viewName);
            return (Filter) filter.accept(visitor, null);
        }

        String prefix;

        public GranuleStoreViewFilterVisitor(String viewName) {
            this.prefix = viewName + ".";
        }

        @Override
        public Object visit(Id filter, Object extraData) {
            Set<Identifier> identifiers = filter.getIdentifiers();
            Set<Identifier> renamedIdentifiers =
                    identifiers
                            .stream()
                            .map(
                                    id -> {
                                        String name = id.getID().toString();
                                        if (name.startsWith(prefix)) {
                                            name = name.substring(prefix.length());
                                        }
                                        return getFactory(extraData).featureId(name);
                                    })
                            .collect(Collectors.toSet());
            return getFactory(extraData).id(renamedIdentifiers);
        }
    }

    static class GranuleStoreView implements GranuleStore {

        private StructuredGridCoverage2DReader reader;

        private CoverageView coverageView;

        private String name;

        private boolean readOnly;

        public GranuleStoreView(
                StructuredGridCoverage2DReader structuredDelegate,
                String referenceName,
                CoverageView coverageView,
                boolean readOnly)
                throws UnsupportedOperationException, IOException {
            this.reader = structuredDelegate;
            this.coverageView = coverageView;
            this.name = referenceName;
            this.readOnly = readOnly;
        }

        @Override
        public SimpleFeatureCollection getGranules(Query q) throws IOException {
            List<CoverageBand> bands = coverageView.getCoverageBands();
            Query renamedQuery = q != null ? new Query(q) : new Query();
            if (q != null && q.getFilter() != null) {
                Filter unmapped =
                        GranuleStoreViewFilterVisitor.unmapIdentifiers(
                                q.getFilter(), coverageView.getName());
                renamedQuery.setFilter(unmapped);
            }
            List<SimpleFeatureCollection> collections = new ArrayList<>();
            boolean returnOnlyFirst =
                    Boolean.TRUE.equals(
                            renamedQuery.getHints().getOrDefault(QUERY_FIRST_BAND, false));
            Set<String> queriesCoverages = new HashSet<>();
            for (CoverageBand band : bands) {
                String coverageName = band.getInputCoverageBands().get(0).getCoverageName();
                // do not query the same source multiple times
                if (queriesCoverages.add(coverageName)) {
                    renamedQuery.setTypeName(coverageName);
                    SimpleFeatureCollection collection =
                            reader.getGranules(coverageName, readOnly).getGranules(renamedQuery);
                    collections.add(collection);
                }

                if (returnOnlyFirst) {
                    break;
                }
            }
            // aggregate and return
            SimpleFeatureCollection result;
            if (collections.size() == 0) {
                throw new IllegalStateException(
                        "Unexpected, there is not a single band in the definition?");
            } else {
                // need to composite the collections
                SimpleFeatureType schema = collections.get(0).getSchema();
                result = new CompositeFeatureCollection(collections, schema);
                // cannot use a simple retyper here, all features need a unique feature id
                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.init(schema);
                tb.setName(coverageView.getName());
                return new RetypingFeatureCollection(result, tb.buildFeatureType()) {
                    @Override
                    public SimpleFeatureIterator features() {
                        return new RetypingIterator(delegate.features(), target) {
                            @Override
                            public SimpleFeature next() {
                                SimpleFeature next = delegate.next();
                                builder.init(next);
                                String newId = coverageView.getName() + "." + next.getID();
                                return builder.buildFeature(newId);
                            }
                        };
                    }
                };
            }
        }

        @Override
        public int getCount(Query q) throws IOException {
            return getGranules(q).size();
        }

        @Override
        public ReferencedEnvelope getBounds(Query q) throws IOException {
            return getGranules(q).getBounds();
        }

        @Override
        public SimpleFeatureType getSchema() throws IOException {
            List<CoverageBand> bands = coverageView.getCoverageBands();
            String coverageName = bands.get(0).getInputCoverageBands().get(0).getCoverageName();
            return reader.getGranules(coverageName, true).getSchema();
        }

        @Override
        public void dispose() throws IOException {
            // TODO: check if we need to dispose it or not
            // Does nothing, the catalog should be disposed by the user
        }

        @Override
        public void addGranules(SimpleFeatureCollection granules) {
            // We deal with the one from the underlying reader
            throw new UnsupportedOperationException();
        }

        public int removeGranules(Filter filter) {
            return removeGranules(filter, new Hints());
        }

        @Override
        public int removeGranules(Filter filter, Hints hints) {
            // unmap the feature identifiers
            Filter unmapped =
                    GranuleStoreViewFilterVisitor.unmapIdentifiers(filter, coverageView.getName());

            List<CoverageBand> bands = coverageView.getCoverageBands();
            int removed = 0;
            for (CoverageBand band : bands) {
                String coverageName = band.getInputCoverageBands().get(0).getCoverageName();
                GranuleStore granuleStore;
                try {
                    granuleStore = (GranuleStore) reader.getGranules(coverageName, false);
                    // TODO: We may revisit the #removed granules computation to take into
                    // account cases where we remove different number of records across different
                    // input coverages
                    removed = granuleStore.removeGranules(unmapped, hints);
                } catch (UnsupportedOperationException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                } catch (IOException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
            return removed;
        }

        @Override
        public void updateGranules(
                String[] attributeNames, Object[] attributeValues, Filter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Transaction getTransaction() {
            return null;
        }

        @Override
        public void setTransaction(Transaction transaction) {}
    }

    private StructuredGridCoverage2DReader structuredDelegate;

    public StructuredCoverageViewReader(
            StructuredGridCoverage2DReader delegate,
            CoverageView coverageView,
            CoverageInfo coverageInfo,
            Hints hints) {
        super(delegate, coverageView, coverageInfo, hints);
        structuredDelegate = delegate;
    }

    @Override
    public GranuleSource getGranules(String coverageName, boolean readOnly)
            throws IOException, UnsupportedOperationException {
        return new GranuleStoreView(structuredDelegate, referenceName, coverageView, readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return structuredDelegate.isReadOnly();
    }

    @Override
    public void createCoverage(String coverageName, SimpleFeatureType schema)
            throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Operation unavailable for Coverage Views");
    }

    @Override
    public boolean removeCoverage(String coverageName, boolean delete)
            throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Operation unavailable for Coverage Views");
    }

    @Override
    public void delete(boolean deleteData) throws IOException {
        structuredDelegate.delete(deleteData);
    }

    @Override
    public List<HarvestedSource> harvest(String defaultTargetCoverage, Object source, Hints hints)
            throws IOException, UnsupportedOperationException {
        return structuredDelegate.harvest(defaultTargetCoverage, source, hints);
    }

    @Override
    public List<DimensionDescriptor> getDimensionDescriptors(String coverageName)
            throws IOException {
        return structuredDelegate.getDimensionDescriptors(referenceName);
    }
}
