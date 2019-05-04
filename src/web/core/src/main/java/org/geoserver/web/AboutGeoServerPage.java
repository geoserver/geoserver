/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.logging.Level;
import org.apache.wicket.markup.html.basic.Label;
import org.geotools.util.factory.GeoTools;

/**
 * An about GeoServer page providing various bits of information.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AboutGeoServerPage extends GeoServerBasePage {

    public AboutGeoServerPage() {
        add(new Label("geotoolsVersion", GeoTools.getVersion().toString()));
        add(new Label("geotoolsRevision", GeoTools.getBuildRevision()));
        add(new Label("geowebcacheVersion", getGwcVersion()));
        add(new Label("geowebcacheRevision", getGwcRevision()));
    }

    public String getGwcVersion() {
        Package p = lookupGwcPackage();
        return p != null ? p.getSpecificationVersion() : null;
    }

    public String getGwcRevision() {
        Package p = lookupGwcPackage();
        return p != null ? p.getImplementationVersion() : null;
    }

    @SuppressWarnings("deprecation") // ClassLoader.getDefinedPackage replaces in jdk 9+
    Package lookupGwcPackage() {
        try {
            return Package.getPackage("org.geowebcache");
        } catch (Exception e) {
            // be safe
            LOGGER.log(Level.FINE, "Error looking up org.geowebcache package", e);
        }
        return null;
    }
}
