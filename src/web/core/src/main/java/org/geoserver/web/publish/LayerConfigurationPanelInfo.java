/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;

/**
 * Extension point for sections of the configuration pages for individual layers.
 *
 * @author David Winslow <dwinslow@openplans.org>
 * @author Niels Charlier
 */
public class LayerConfigurationPanelInfo extends PublishedConfigurationPanelInfo<LayerInfo> {
    public static final long serialVersionUID = -1l;

    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.config");

    private List<String> myHandleableClasses;

    @Override
    public Class<LayerInfo> getPublishedInfoClass() {
        return LayerInfo.class;
    }

    public void setSupportedTypes(List<String> types) {
        myHandleableClasses = types;
    }

    public List<String> getSupportedTypes() {
        return Collections.unmodifiableList(myHandleableClasses);
    }

    @Override
    public boolean canHandle(PublishedInfo layer) {
        if (super.canHandle(layer)) {
            if (myHandleableClasses == null) {
                return true;
            }

            for (String className : myHandleableClasses) {
                try {
                    if (Class.forName(className).isInstance(((LayerInfo) layer).getResource())) {
                        return true;
                    }
                } catch (ClassNotFoundException cnfe) {
                    LOGGER.severe(
                            "Couldn't find class "
                                    + className
                                    + "; please check your applicationContext.xml");
                }
            }
        }
        return false;
    }
}
