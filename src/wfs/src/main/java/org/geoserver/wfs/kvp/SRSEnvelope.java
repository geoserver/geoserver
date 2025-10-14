/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.io.Serial;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

/**
 * A {@link ReferencedEnvelope} subclass that keeps the original SRS after the KVP parse, to ensure we are true to the
 * original SRS when building a BBOX filter
 *
 * @author Andrea Aime - GeoSolutions
 */
class SRSEnvelope extends ReferencedEnvelope {
    @Serial
    private static final long serialVersionUID = 4510785331988235178L;

    String srs;

    public SRSEnvelope(double x1, double x2, double y1, double y2, String srs)
            throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException {
        super(x1, x2, y1, y2, srs != null ? CRS.decode(srs) : null);
        this.srs = srs;
    }

    public String getSrs() {
        return srs;
    }
}
