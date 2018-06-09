/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.ComponentTag;
import org.geoserver.importer.Importer;
import org.geoserver.web.GeoServerApplication;

/**
 * Importer web utilities.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ImporterWebUtils {

    static Importer importer() {
        return GeoServerApplication.get().getBeanOfType(Importer.class);
    }

    static boolean isDevMode() {
        return RuntimeConfigurationType.DEVELOPMENT
                == GeoServerApplication.get().getConfigurationType();
    }

    static void disableLink(ComponentTag tag) {
        tag.setName("a");
        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
    }
}
