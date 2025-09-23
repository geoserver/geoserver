/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.ResourcePool;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;

/**
 * A model allowing to edit an SRS property with the CRSPanel (by dynamically converting it into a
 * {@link CoordinateReferenceSystem} and back)
 */
public class SRSToCRSModel implements IModel<CoordinateReferenceSystem> {
    @Serial
    private static final long serialVersionUID = 1887687559796645124L;

    private static final Logger LOGGER = Logging.getLogger(SRSToCRSModel.class);
    IModel<String> srsModel;

    public SRSToCRSModel(IModel<String> srsModel) {
        this.srsModel = srsModel;
    }

    @Override
    public CoordinateReferenceSystem getObject() {
        String srs = srsModel.getObject();
        if (srs == null || "UNKNOWN".equals(srs)) return null;
        try {
            return CRS.decode(srs);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setObject(CoordinateReferenceSystem object) {
        CoordinateReferenceSystem crs = object;
        try {
            String srs = ResourcePool.lookupIdentifier(object, false);
            srsModel.setObject(srs);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Failed to lookup the SRS code for " + crs);
            srsModel.setObject(null);
        }
    }

    @Override
    public void detach() {
        srsModel.detach();
    }
}
