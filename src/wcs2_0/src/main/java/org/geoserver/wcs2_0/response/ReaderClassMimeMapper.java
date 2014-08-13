/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;

import org.geoserver.catalog.CoverageInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverageReader;

/**
 * An implementation of {@link CoverageMimeTypeMapper} based on the {@link GridCoverage2D} class
 * associated to the CoverageInfo
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ReaderClassMimeMapper implements CoverageMimeTypeMapper {

    private String mime;

    private Class<? extends GridCoverageReader> readerClass;

    public ReaderClassMimeMapper(Class<? extends GridCoverageReader> readerClass, String mime) {
        this.readerClass = readerClass;
        this.mime = mime;
    }

    @Override
    public String getMimeType(CoverageInfo ci) throws IOException {
        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        if (reader != null && readerClass.isAssignableFrom(reader.getClass())) {
            return mime;
        }

        return null;
    }
}
