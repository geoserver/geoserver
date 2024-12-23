/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.autopopulate;

import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;

/**
 * AutopopulateTemplateLoader class is used to load the template from the file system and return the
 * AutopopulateTemplate object.
 *
 * <p>It also provides the methods to load the template from the file system.
 *
 * @author Alessio Fabiani, GeoSolutions SRL, alessio.fabiani@geosolutionsgroup.com
 */
public class AutopopulateTemplateLoader {

    /** logger */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(AutopopulateTemplateLoader.class);
    /** Feature type directory to load template against. Its presence is mutually exclusive with coverageName */
    protected ResourceInfo resource;
    /** GeoServer data directory */
    GeoServerDataDirectory dd;

    /**
     * Constructs the template loader.
     *
     * @param rl The geoserver resource loader
     * @param resource The resource to load the template from
     */
    public AutopopulateTemplateLoader(GeoServerResourceLoader rl, ResourceInfo resource) {
        this(
                rl == null
                        ? new GeoServerDataDirectory(GeoServerExtensions.bean(GeoServerResourceLoader.class))
                        : new GeoServerDataDirectory(rl),
                resource);
    }

    /**
     * Constructs the template loader.
     *
     * @param dd The geoserver data directory
     * @param resource The resource to load the template from
     */
    public AutopopulateTemplateLoader(GeoServerDataDirectory dd, ResourceInfo resource) {
        this.dd = dd;
        this.resource = resource;
    }

    /**
     * Load the template from the file system.
     *
     * @param path The path to the template
     * @return The AutopopulateTemplate object
     * @throws IOException If the template cannot be loaded
     */
    public AutopopulateTemplate loadTemplate(String path) throws IOException {
        Resource template = null;

        // Template looks up relative to resource
        if (resource != null) {
            template = dd.get(resource, path);
            LOGGER.fine("Template looks up relative to resource " + template.path());
            return new AutopopulateTemplate(template);
        }

        LOGGER.warning("No Resource found!");
        return null;
    }
}
