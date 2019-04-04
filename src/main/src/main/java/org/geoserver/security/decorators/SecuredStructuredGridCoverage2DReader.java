/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import org.geoserver.security.WrapperPolicy;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;

/**
 * Applies access limits policies around the wrapped reader
 *
 * @author Daniele Romagnoli - GeoSolutions
 */
public class SecuredStructuredGridCoverage2DReader
        extends DecoratingStructuredGridCoverage2DReader {

    WrapperPolicy policy;

    public SecuredStructuredGridCoverage2DReader(
            StructuredGridCoverage2DReader delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    public Format getFormat() {
        Format format = delegate.getFormat();
        if (format == null) {
            return null;
        } else {
            return (Format) SecuredObjects.secure(format, policy);
        }
    }

    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return SecuredGridCoverage2DReader.read(delegate, policy, parameters);
    }

    @Override
    public ServiceInfo getInfo() {
        ServiceInfo info = delegate.getInfo();
        if (info == null) {
            return null;
        } else {
            return (ServiceInfo) SecuredObjects.secure(info, policy);
        }
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        ResourceInfo info = delegate.getInfo(coverageName);
        if (info == null) {
            return null;
        } else {
            return (ResourceInfo) SecuredObjects.secure(info, policy);
        }
    }
}
