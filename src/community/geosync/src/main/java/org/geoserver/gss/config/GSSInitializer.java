package org.geoserver.gss.config;

import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfo.Version;
import org.geotools.util.logging.Logging;

public class GSSInitializer implements GeoServerInitializer {

    private static final Logger LOGGER = Logging.getLogger(GSSInitializer.class);

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        WFSInfo wfs = geoServer.getService(WFSInfo.class);

        final Version[] wfsVersions = WFSInfo.Version.values();
        boolean changed = false;
        for (Version wfsVersion : wfsVersions) {
            // set the defaults for GMLInfo if they are not set
            GMLInfo gml = wfs.getGML().get(wfsVersion);
            if (gml != null) {
                changed |= verifyGmlOverrideAttributes(gml);
            }
        }

        if (changed) {
            geoServer.save(wfs);
            LOGGER.warning("****************************************************************************");
            LOGGER.warning("The GeoSync Service initializer changed the configuration of the WFS service");
            LOGGER.warning("by setting the GML 'override GML attributes' config option to TRUE. This is");
            LOGGER.warning("needed for the replication of FeatureTypes to carry over the complete schema");
            LOGGER.warning("instead of assuming the replicating client will assume, for excample, a 'name'");
            LOGGER.warning("attribute does not need to be declared because it maps to gml:name.");
            LOGGER.warning("****************************************************************************");
        }
    }

    private boolean verifyGmlOverrideAttributes(GMLInfo gml) {
        Boolean overrideGMLAttributes = gml.getOverrideGMLAttributes();
        if (overrideGMLAttributes == null || Boolean.FALSE.equals(overrideGMLAttributes)) {
            gml.setOverrideGMLAttributes(true);
            return true;
        }
        return false;
    }

}
