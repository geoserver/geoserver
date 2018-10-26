/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Delegates every method to the delegate grid format. Subclasses will override selected methods to
 * perform their "decoration" job
 *
 * @author Andrea Aime
 */
public abstract class DecoratingGridFormat implements Format {

    AbstractGridFormat delegate;

    public DecoratingGridFormat(AbstractGridFormat delegate) {
        this.delegate = delegate;
    }

    public boolean accepts(Object source, Hints hints) {
        return delegate.accepts(source, hints);
    }

    public boolean accepts(Object source) {
        return delegate.accepts(source);
    }

    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        return delegate.getDefaultImageIOWriteParameters();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public String getDocURL() {
        return delegate.getDocURL();
    }

    public String getName() {
        return delegate.getName();
    }

    public GridCoverage2DReader getReader(Object source, Hints hints) {
        return delegate.getReader(source, hints);
    }

    public GridCoverage2DReader getReader(Object source) {
        return delegate.getReader(source);
    }

    public ParameterValueGroup getReadParameters() {
        return delegate.getReadParameters();
    }

    public String getVendor() {
        return delegate.getVendor();
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public ParameterValueGroup getWriteParameters() {
        return delegate.getWriteParameters();
    }

    public GridCoverageWriter getWriter(Object destination, Hints hints) {
        return delegate.getWriter(destination, hints);
    }

    public GridCoverageWriter getWriter(Object destination) {
        return delegate.getWriter(destination);
    }
}
