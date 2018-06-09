/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import com.thoughtworks.xstream.XStream;
import org.geoserver.ogr.core.AbstractToolConfigurator;
import org.geoserver.ogr.core.ToolConfiguration;
import org.geoserver.ogr.core.ToolWrapperFactory;

/**
 * Loads the ogr2ogr.xml configuration file and configures the output format accordingly.
 *
 * <p>Also keeps tabs on the configuration file, reloading the file as needed.
 *
 * @author Administrator
 */
public class Ogr2OgrConfigurator extends AbstractToolConfigurator {

    public Ogr2OgrConfigurator(Ogr2OgrOutputFormat format, ToolWrapperFactory wrapperFactory) {
        super(format, wrapperFactory);
    }

    @Override
    protected String getConfigurationFile() {
        return "ogr2ogr.xml";
    }

    @Override
    protected ToolConfiguration getDefaultConfiguration() {
        return OgrConfiguration.DEFAULT;
    }

    /** Ensures compatibility with old style configuration files. */
    @Override
    protected XStream buildXStream() {
        XStream xstream = super.buildXStream();
        // setup OGR-specific aliases
        xstream.alias("OgrConfiguration", OgrConfiguration.class);
        xstream.alias("Format", OgrFormat.class);
        xstream.allowTypes(new Class[] {OgrConfiguration.class, OgrFormat.class});

        return xstream;
    }
}
