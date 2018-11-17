/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.util.ProgressListener;

/**
 * A raster-based or coverage based resource.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @uml.dependency supplier="org.geoserver.catalog.CoverageResource"
 */
public interface CoverageInfo extends ResourceInfo {

    /**
     * The data store the feature type is a part of.
     *
     * <p>
     */
    CoverageStoreInfo getStore();

    /**
     * The native format of the coverage.
     *
     * @uml.property name="nativeFormat"
     */
    String getNativeFormat();

    /**
     * Sets the native format of the coverage.
     *
     * @uml.property name="nativeFormat"
     */
    void setNativeFormat(String nativeFormat);

    /**
     * The supported formats for the coverage.
     *
     * @uml.property name="supportedFormats"
     */
    List<String> getSupportedFormats();

    /**
     * The collection of identifiers of the crs's the coverage supports in a request.
     *
     * @uml.property name="requestSRS"
     */
    List<String> getRequestSRS();

    /**
     * The collection of identifiers of the crs's the coverage supports in a response.
     *
     * @uml.property name="responseSRS"
     */
    List<String> getResponseSRS();

    /**
     * The default interpolation method for hte coverage.
     *
     * @uml.property name="defaultInterpolationMethod"
     */
    String getDefaultInterpolationMethod();

    /**
     * Sets the default interpolation method for the coverage.
     *
     * @uml.property name="defaultInterpolationMethod"
     */
    void setDefaultInterpolationMethod(String defaultInterpolationMethod);

    /**
     * The collection of interpolation methods available for the coverage.
     *
     * @uml.property name="interpolationMethods"
     */
    List<String> getInterpolationMethods();

    /**
     * A map of coverage specific parameters.
     *
     * @uml.property name="parameters"
     */
    Map<String, Serializable> getParameters();

    /**
     * The dimensions of the coverage.
     *
     * @uml.property name="dimensions"
     * @uml.associationEnd multiplicity="(0 -1)" container="java.util.List"
     *     inverse="coverageInfo:org.geoserver.catalog.CoverageDimension"
     */
    List<CoverageDimensionInfo> getDimensions();

    /** The grid geometry. */
    GridGeometry getGrid();

    /** Sets the grid geometry. */
    void setGrid(GridGeometry grid);

    /**
     * Returns the underlying grid coverage instance.
     *
     * <p>This method does I/O and is potentially blocking. The <tt>listener</tt> may be used to
     * report the progress of loading the coverage and also to report any errors or warnings that
     * occur.
     *
     * @param listener A progress listener, may be <code>null</code>.
     * @param hints Hints to be used when loading the coverage.
     * @return The grid coverage.
     * @throws IOException Any I/O problems.
     */
    GridCoverage getGridCoverage(ProgressListener listener, Hints hints) throws IOException;

    GridCoverage getGridCoverage(
            ProgressListener listener, ReferencedEnvelope envelope, Hints hints) throws IOException;

    GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException;

    /** Returns the native coverage name (might be null for single coverage formats) */
    String getNativeCoverageName();

    /** Sets the native coverage name (used to pick up a specific coverage from withing a reader) */
    void setNativeCoverageName(String nativeCoverageName);
}
