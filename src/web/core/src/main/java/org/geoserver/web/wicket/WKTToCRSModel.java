/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.IModel;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;

/**
 * A model allowing to edit an WKT property with the CRSPanel (by dynamically converting it into a
 * {@link CoordinateReferenceSystem} and back)
 */
@SuppressWarnings("serial")
public class WKTToCRSModel implements IModel<CoordinateReferenceSystem> {
    private static final Logger LOGGER = Logging.getLogger(WKTToCRSModel.class);
    IModel<String> srsModel;

    public WKTToCRSModel(IModel<String> srsModel) {
        this.srsModel = srsModel;
    }

    @Override
    public CoordinateReferenceSystem getObject() {
        String wkt = srsModel.getObject();
        try {
            return CRS.parseWKT(wkt);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setObject(CoordinateReferenceSystem object) {
        CoordinateReferenceSystem crs = object;
        try {
            srsModel.setObject(crs.toString());
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
