/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.ServiceInfo;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;

/**
 * Delegates every method to the delegate grid coverage reader. Subclasses will
 * override selected methods to perform their "decoration" job
 * 
 * @author Andrea Aime
 */
public abstract class DecoratingGridCoverage2DReader extends AbstractGridCoverage2DReader {

    AbstractGridCoverage2DReader delegate;

    public DecoratingGridCoverage2DReader(AbstractGridCoverage2DReader delegate) {
        this.delegate = delegate;
        this.crs = delegate.getCrs();
        this.originalEnvelope = delegate.getOriginalEnvelope();
        this.originalGridRange = delegate.getOriginalGridRange();
    }

    public void dispose() {
        delegate.dispose();
    }

    public String getCurrentSubname() {
        return delegate.getCurrentSubname();
    }

    public Format getFormat() {
        return delegate.getFormat();
    }

    public int getGridCoverageCount() {
        return delegate.getGridCoverageCount();
    }

    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    public String[] getMetadataNames() {
        return delegate.getMetadataNames();
    }

    public String getMetadataValue(String name) {
        return delegate.getMetadataValue(name);
    }

    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution) {
        return delegate.getReadingResolutions(policy, requestedResolution);
    }

    public boolean hasMoreGridCoverages() {
        return delegate.hasMoreGridCoverages();
    }

    public String[] listSubNames() {
        return delegate.listSubNames();
    }

    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IllegalArgumentException,
            IOException {
        return delegate.read(parameters);
    }

    public void skip() {
        delegate.skip();
    }

    
    
    
}
