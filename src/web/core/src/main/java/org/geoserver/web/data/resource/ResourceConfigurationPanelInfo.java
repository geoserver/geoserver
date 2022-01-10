/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.web.ComponentInfo;

/**
 * Extension point for sections of the configuration pages for individual resources.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class ResourceConfigurationPanelInfo extends ComponentInfo<ResourceConfigurationPanel> {
    public static final long serialVersionUID = -1l;

    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.config");

    private List<String> myHandleableClasses;

    public void setSupportedTypes(List<String> classNames) {
        myHandleableClasses = classNames;
    }

    public List<String> getSupportedTypes() {
        return Collections.unmodifiableList(myHandleableClasses);
    }

    public boolean canHandle(Object obj) {
        if (myHandleableClasses == null) return true;

        for (String className : myHandleableClasses) {
            try {
                if (Class.forName(className).isInstance(obj)) {
                    return true;
                }
            } catch (ClassNotFoundException cnfe) {
                LOGGER.severe(
                        "Couldn't find class "
                                + className
                                + "; please check your applicationContext.xml.");
            }
        }
        return false;
    }
}
