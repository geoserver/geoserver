/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Utility class that maps the coverage data sets and child coverage names
 *
 * @author Andrea Aime - GeoSolutions
 */
public class EOCoverageResourceCodec {
    private static Logger LOGGER = Logging.getLogger(EOCoverageResourceCodec.class);

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory2();

    private static final String DATASET_SUFFIX = "_dss";

    private static final String GRANULE_SEPARATOR = "_granule_";

    private Catalog catalog;

    public EOCoverageResourceCodec(Catalog catalog) {
        this.catalog = catalog;
    }

    public String getDatasetName(CoverageInfo ci) {
        if (!isValidDataset(ci)) {
            throw new IllegalArgumentException(
                    "Specified covearge " + ci.prefixedName() + " is not a valid EO dataset");
        }

        return NCNameResourceCodec.encode(ci) + DATASET_SUFFIX;
    }

    /**
     * Checks if the specified coverage is a valid dataset, e.g., it has the dataset flag enabled
     * and time dimension, and has a structured grid coverage reader backing it
     */
    public boolean isValidDataset(CoverageInfo ci) {
        Boolean dataset = ci.getMetadata().get(WCSEOMetadata.DATASET.key, Boolean.class);
        DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        try {
            GridCoverageReader reader = ci.getGridCoverageReader(null, GeoTools.getDefaultHints());
            boolean structured = reader instanceof StructuredGridCoverage2DReader;
            return dataset != null && dataset && time != null && time.isEnabled() && structured;
        } catch (IOException e) {
            throw new ServiceException(
                    "Failed to locate the grid coverage reader for coverage " + ci.prefixedName());
        }
    }

    /**
     * Returns the coverage backed by the provided datasetId
     *
     * @return the coverage, or null if not found, or if not a coverage
     */
    public CoverageInfo getDatasetCoverage(String datasetId) {
        if (!datasetId.endsWith(DATASET_SUFFIX)) {
            LOGGER.fine(
                    "Invalid dataset id " + datasetId + " it does not end with " + DATASET_SUFFIX);
            return null;
        }

        String coverageName = datasetId.substring(0, datasetId.length() - DATASET_SUFFIX.length());
        LayerInfo layer = NCNameResourceCodec.getCoverage(catalog, coverageName);
        if (layer == null) {
            LOGGER.fine(
                    "Invalid dataset id " + datasetId + " does not match any published dataset");
            return null;
        }
        CoverageInfo ci = (CoverageInfo) layer.getResource();
        if (!isValidDataset(ci)) {
            LOGGER.fine(
                    "Invalid dataset id " + datasetId + " does not match any published dataset");
            return null;
        }

        return ci;
    }

    /** Builds the identifier for a granule inside a coverage */
    public String getGranuleId(CoverageInfo coverage, String featureId) {
        return NCNameResourceCodec.encode(coverage) + GRANULE_SEPARATOR + featureId;
    }

    /**
     * Returns the coverage containing the specified coverage, or null if the syntax is incorrect,
     * the coverage does not exist, or it's not a dataset
     */
    public CoverageInfo getGranuleCoverage(String granuleId) {
        // does it have the expected lexical structure?
        if (!granuleId.contains(GRANULE_SEPARATOR)) {
            return null;
        }
        String[] splitted = granuleId.split(GRANULE_SEPARATOR);
        if (splitted.length != 2) {
            return null;
        }

        // do we have the coverage?
        LayerInfo li = NCNameResourceCodec.getCoverage(catalog, splitted[0]);
        if (li == null) {
            return null;
        }

        // is it a EO dataset?
        CoverageInfo ci = (CoverageInfo) li.getResource();
        if (isValidDataset(ci)) {
            return ci;
        } else {
            return null;
        }
    }

    /**
     * Given a valid granule id returns a Filter to extract it from the structured grid coverage
     * reader
     */
    public Filter getGranuleFilter(String granuleId) {
        // does it have the expected lexical structure?
        if (!granuleId.contains(GRANULE_SEPARATOR)) {
            throw new IllegalArgumentException("Not a valid granule id: " + granuleId);
        }
        String[] splitted = granuleId.split(GRANULE_SEPARATOR);
        if (splitted.length != 2) {
            throw new IllegalArgumentException("Not a valid granule id: " + granuleId);
        }

        return FF.id(Collections.singleton(FF.featureId(splitted[1])));
    }

    public String getCoverageName(CoverageInfo ci) throws IOException {
        return ci.getNativeCoverageName() != null
                ? ci.getNativeCoverageName()
                : ci.getGridCoverageReader(null, null).getGridCoverageNames()[0];
    }
}
