/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.model.IModel;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Model wrapper for {@link CoordinateReferenceSystem} instances.
 *
 * <p>This model operates by persisting the wkt ({@link CoordinateReferenceSystem#toWKT()}) for a
 * crs.
 *
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class CRSModel implements IModel<CoordinateReferenceSystem> {

    transient CoordinateReferenceSystem crs;
    String wkt;

    public CRSModel(CoordinateReferenceSystem crs) {
        setObject(crs);
    }

    public CoordinateReferenceSystem getObject() {
        if (crs != null) {
            return crs;
        }

        if (wkt == null) {
            return null;
        }

        try {
            crs = CRS.parseWKT(wkt);
            return crs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setObject(CoordinateReferenceSystem object) {
        this.crs = object;
        this.wkt = crs != null ? crs.toWKT() : null;
    }

    public void detach() {
        crs = null;
    }
}
