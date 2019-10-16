/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import org.geoserver.catalog.CoverageInfo;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Allows implementor to listen to the addition/removal of granules in a {@link
 * org.geotools.coverage.grid.io.StructuredGridCoverage2DReader}.
 *
 * <p>This is a temporary setup, if the checkpoint service survives experimentation it should be
 * replaced by events directly issued by the {@link org.geoserver.catalog.ResourcePool}
 */
public interface ImageListener {

    /**
     * An image has been added to the to <code>ci</code>
     *
     * @param ci The coverage backed by a StructuredGridCoverage2DReader
     * @param feature The feature representing the image in the {@link
     *     org.geotools.coverage.grid.io.GranuleSource}
     */
    void imageAdded(CoverageInfo ci, SimpleFeature feature);

    /**
     * An image has been removed from the to <code>ci</code>
     *
     * @param ci The coverage backed by a StructuredGridCoverage2DReader
     * @param feature The feature representing the image in the {@link
     *     org.geotools.coverage.grid.io.GranuleSource}
     */
    void imageRemoved(CoverageInfo ci, SimpleFeature feature);

    // TODO: eventually handle image change too, for the moment that is not allowed by the
    // ImagesService implementation
}
