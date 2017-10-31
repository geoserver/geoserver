/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.feature.CompositeFeatureCollection;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.AbstractFeatureVisitor;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DefaultProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * A coverageView reader using a structured coverage readers implementation
 * 
 * @author Daniele Romagnoli - GeoSolutions
 */
public class StructuredCoverageViewReader extends CoverageViewReader implements
        StructuredGridCoverage2DReader {

    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(StructuredCoverageViewReader.class);

    static class GranuleStoreView implements GranuleStore {

        private StructuredGridCoverage2DReader reader;

        private CoverageView coverageView;

        private String name;

        private boolean readOnly;

        public GranuleStoreView(StructuredGridCoverage2DReader structuredDelegate,
                String referenceName, CoverageView coverageView, boolean readOnly)
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
            List<SimpleFeatureCollection> collections = new ArrayList<>();
            for (CoverageBand band : bands) {
                String coverageName = band.getInputCoverageBands().get(0).getCoverageName();
                renamedQuery.setTypeName(coverageName);
                SimpleFeatureCollection collection = reader.getGranules(coverageName, readOnly).getGranules(renamedQuery);
                collections.add(collection);
            }
            // aggregate and return
            if (collections.size() == 0) {
                throw new IllegalStateException("Unexpected, there is not a single band in the definition?");
            } else if (collections.size() == 1) {
                return collections.get(0);
            } else {
                return new CompositeFeatureCollection(collections);
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

        @Override
        public int removeGranules(Filter filter) {
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
                    removed = granuleStore.removeGranules(filter);
                } catch (UnsupportedOperationException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                } catch (IOException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                }
            }
            return removed;
        }

        @Override
        public void updateGranules(String[] attributeNames, Object[] attributeValues, Filter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Transaction getTransaction() {
            return null;
        }

        @Override
        public void setTransaction(Transaction transaction) {

        }
    }

    private StructuredGridCoverage2DReader structuredDelegate;

    public StructuredCoverageViewReader(StructuredGridCoverage2DReader delegate,
            CoverageView coverageView, CoverageInfo coverageInfo, Hints hints) {
        super(delegate, coverageView, coverageInfo, hints);
        structuredDelegate = delegate;
    }

    @Override
    public GranuleSource getGranules(String coverageName, boolean readOnly) throws IOException,
            UnsupportedOperationException {
        return new GranuleStoreView(structuredDelegate, referenceName, coverageView, readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return structuredDelegate.isReadOnly();
    }

    @Override
    public void createCoverage(String coverageName, SimpleFeatureType schema) throws IOException,
            UnsupportedOperationException {
        throw new UnsupportedOperationException("Operation unavailable for Coverage Views");
    }

    @Override
    public boolean removeCoverage(String coverageName) throws IOException,
            UnsupportedOperationException {
        return removeCoverage(referenceName, false);
    }

    @Override
    public boolean removeCoverage(String coverageName, boolean delete) throws IOException,
            UnsupportedOperationException {
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
