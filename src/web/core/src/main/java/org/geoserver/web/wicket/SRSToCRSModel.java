/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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
 * A model allowing to edit an SRS property with the CRSPanel (by dynamically
 * converting it into a {@link CoordinateReferenceSystem} and back)
 */
@SuppressWarnings("serial")
public class SRSToCRSModel implements IModel {
    private static final Logger LOGGER = Logging.getLogger(SRSToCRSModel.class);
    IModel srsModel; 
    
    public SRSToCRSModel(IModel srsModel) {
        this.srsModel = srsModel;
    }

    public Object getObject() {
        String srs = (String) srsModel.getObject();
        if(srs == null || "UNKNOWN".equals(srs))
            return null;
        try {
            return CRS.decode(srs);
        } catch(Exception e) {
            return null;
        }
    }

    public void setObject(Object object) {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) object;
        try {
            Integer epsgCode = CRS.lookupEpsgCode(crs, false);
            String srs = epsgCode != null ? "EPSG:" + epsgCode : null;
            srsModel.setObject(srs);
        } catch(Exception e) {
            LOGGER.log(Level.INFO, "Failed to lookup the SRS code for " + crs);
            srsModel.setObject(null);
        }
        
    }

    public void detach() {
        srsModel.detach();
    }
    
}
