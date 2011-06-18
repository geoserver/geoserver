package org.geoserver.web;

import org.apache.wicket.markup.html.basic.Label;
import org.geotools.factory.GeoTools;

/**
 * An about GeoServer page providing various bits of information.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AboutGeoServerPage extends GeoServerBasePage {

    public AboutGeoServerPage() {
        add(new Label("geotoolsVersion", GeoTools.getVersion().toString()));
        add(new Label("geotoolsRevision", GeoTools.getBuildRevision().toString()));
    }
}
