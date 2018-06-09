/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.IModel;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    public CoordinateReferenceSystem getObject() {
        String wkt = srsModel.getObject();
        try {
            return CRS.parseWKT(wkt);
        } catch (Exception e) {
            return null;
        }
    }

    public void setObject(CoordinateReferenceSystem object) {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) object;
        try {
            srsModel.setObject(crs.toString());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Failed to lookup the SRS code for " + crs);
            srsModel.setObject(null);
        }
    }

    public void detach() {
        srsModel.detach();
    }
}
