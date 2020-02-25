/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.factory.Hints;

/**
 * Secures a format applying the policy
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredGridFormat extends DecoratingGridFormat {

    WrapperPolicy policy;

    public SecuredGridFormat(AbstractGridFormat delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    public GridCoverage2DReader getReader(Object source, Hints hints) {
        GridCoverage2DReader reader = delegate.getReader(source, hints);
        if (reader == null) {
            return reader;
        } else {
            return (GridCoverage2DReader) SecuredObjects.secure(reader, policy);
        }
    }

    public GridCoverage2DReader getReader(Object source) {
        GridCoverage2DReader reader = delegate.getReader(source);
        if (reader == null) {
            return reader;
        } else {
            return (GridCoverage2DReader) SecuredObjects.secure(reader, policy);
        }
    }

    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link
     * UnsupportedOperationException} in case we have to conceal the fact the data is actually
     * writable, using an Spring Security security exception otherwise to force an authentication
     * from the user
     */
    RuntimeException notifyUnsupportedOperation() {
        if (policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess();
        } else
            return new UnsupportedOperationException(
                    "This data access is read only, service code is supposed to perform writes via FeatureStore instead");
    }
}
