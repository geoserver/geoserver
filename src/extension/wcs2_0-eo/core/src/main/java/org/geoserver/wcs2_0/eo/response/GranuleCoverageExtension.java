/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.eo.EOCoverageResourceCodec;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.WCS20DescribeCoverageExtension;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Extension point implementing {@link WCS20DescribeCoverageExtension} that handles the granules for
 * Structured Coverages
 *
 * @author Nicola Lagomarsini - GeoSolutions
 */
public class GranuleCoverageExtension implements WCS20DescribeCoverageExtension {
    /** Constant used as separator for the granule definition */
    private static final String GRANULE_SEPARATOR = "_granule_";

    private static final Logger LOGGER = Logging.getLogger(GranuleCoverageExtension.class);

    /** Parser used for decoding the coverageId parameter */
    private EOCoverageResourceCodec codec;

    /** GeoServer instance used for checking if the EO extension is enabled */
    private GeoServer geoserver;

    public GranuleCoverageExtension(GeoServer geoServer, EOCoverageResourceCodec codec) {
        this.codec = codec;
        this.geoserver = geoServer;
    }

    @Override
    public String handleCoverageId(String coverageId) {
        if (isEOEnabled()) {
            CoverageInfo granuleCoverage = codec.getGranuleCoverage(coverageId);
            if (granuleCoverage != null && codec.isValidDataset(granuleCoverage)) {
                return getCoverageId(coverageId);
            }
            return coverageId;
        }

        return coverageId;
    }

    @Override
    public String handleEncodedId(String encodedId, String coverageId) {
        if (isEOEnabled()) {
            CoverageInfo granuleCoverage = codec.getGranuleCoverage(coverageId);
            if (granuleCoverage != null && codec.isValidDataset(granuleCoverage)) {
                return codec.getGranuleId(granuleCoverage, getGranuleId(coverageId));
            }
            return coverageId;
        }

        return coverageId;
    }

    @Override
    public CoverageInfo handleCoverageInfo(String coverageId, CoverageInfo ci) {
        CoverageInfo info = null;
        if (isEOEnabled()) {
            if (codec.getGranuleCoverage(coverageId) != null && codec.isValidDataset(ci)) {
                SimpleFeatureIterator it = null;
                try {
                    // Getting the structured coverage reader
                    StructuredGridCoverage2DReader reader =
                            (StructuredGridCoverage2DReader)
                                    ci.getGridCoverageReader(null, GeoTools.getDefaultHints());
                    String name;
                    // Getting the coverage name
                    name = codec.getCoverageName(ci);
                    // Query the reader
                    GranuleSource source = reader.getGranules(name, true);
                    Query q = new Query();
                    SimpleFeatureCollection collection = source.getGranules(q);

                    // create a GranuleCoverageInfo object for the single granule
                    if (!collection.isEmpty()) {
                        List<DimensionDescriptor> descriptors =
                                getActiveDimensionDescriptor(ci, reader, name);
                        it = collection.features();
                        if (it.hasNext()) {
                            SimpleFeature feature = it.next();
                            info = new GranuleCoverageInfo(ci, feature, descriptors);
                        }
                    }
                    if (info == null) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                    Level.FINE,
                                    "No granule found for the granuleId: "
                                            + getGranuleId(coverageId));
                        }
                    }
                } catch (IOException e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                    throw new WCS20Exception(e);
                } finally {
                    if (it != null) {
                        try {
                            it.close();
                        } catch (Exception e) {
                            if (LOGGER.isLoggable(Level.SEVERE)) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
        if (info == null) {
            info = ci;
        }
        return info;
    }

    /**
     * CHecks if the EO extension is enabled globally
     *
     * @return true if the EO extension is enabled
     */
    public boolean isEOEnabled() {
        WCSInfo wcs = geoserver.getService(WCSInfo.class);
        Boolean enabled = wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * Returns the coverage identifier related to the specified coverageId, or null if the syntax is
     * incorrect
     *
     * @return the coverageId related to the following coverageId parameter (with the _granule_
     *     extension)
     */
    public String getCoverageId(String coverageId) {
        // does it have the expected lexical structure?
        if (!coverageId.contains(GRANULE_SEPARATOR)) {
            return null;
        }
        String[] splitted = coverageId.split(GRANULE_SEPARATOR);
        if (splitted.length != 2) {
            return null;
        } else {
            return splitted[0];
        }
    }

    /**
     * Returns the coverage identifier related to the specified coverageId, or null if the syntax is
     * incorrect
     *
     * @return the coverageId related to the following coverageId parameter (with the _granule_
     *     extension)
     */
    public String getGranuleId(String coverageId) {
        // does it have the expected lexical structure?
        if (!coverageId.contains(GRANULE_SEPARATOR)) {
            return null;
        }
        String[] splitted = coverageId.split(GRANULE_SEPARATOR);
        if (splitted.length != 2) {
            return null;
        } else {
            return splitted[1];
        }
    }

    /** This method returns the active dimensions */
    private List<DimensionDescriptor> getActiveDimensionDescriptor(
            CoverageInfo ci, StructuredGridCoverage2DReader reader, String name)
            throws IOException {
        // map the source descriptors for easy retrieval
        Map<String, DimensionDescriptor> sourceDescriptors =
                new HashMap<String, DimensionDescriptor>();
        for (DimensionDescriptor dimensionDescriptor : reader.getDimensionDescriptors(name)) {
            sourceDescriptors.put(dimensionDescriptor.getName().toUpperCase(), dimensionDescriptor);
        }
        // select only those that have been activated vai the GeoServer GUI
        List<DimensionDescriptor> enabledDescriptors = new ArrayList<DimensionDescriptor>();
        for (Entry<String, Serializable> entry : ci.getMetadata().entrySet()) {
            if (entry.getValue() instanceof DimensionInfo) {
                DimensionInfo di = (DimensionInfo) entry.getValue();
                if (di.isEnabled()) {
                    String dimensionName = entry.getKey();
                    if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                        dimensionName =
                                dimensionName.substring(
                                        ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                    }
                    DimensionDescriptor selected =
                            sourceDescriptors.get(dimensionName.toUpperCase());
                    if (selected != null) {
                        enabledDescriptors.add(selected);
                    }
                }
            }
        }

        return enabledDescriptors;
    }
}
