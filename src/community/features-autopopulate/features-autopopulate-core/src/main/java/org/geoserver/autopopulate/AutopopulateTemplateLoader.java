/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.autopopulate;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resources;

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
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(AutopopulateTemplateLoader.class);
    /**
     * Feature type directory to load template against. Its presence is mutually exclusive with
     * coverageName
     */
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
                        ? new GeoServerDataDirectory(
                                GeoServerExtensions.bean(GeoServerResourceLoader.class))
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
        File template = null;

        // template look up order
        // 1. Relative to resource
        // 2. Relative to store of the resource
        // 3. Relative to workspace of resource
        // 4. Relative to workspaces directory
        if (resource != null) {
            // first check relative to set resource
            template = Resources.file(dd.get(resource, path));

            if (template == null) {
                // next try relative to the store
                template = Resources.file(dd.get(resource.getStore(), path));
            }

            if (template == null) {
                // next try relative to the workspace
                template = Resources.file(dd.get(resource.getStore().getWorkspace(), path));
            }

            if (template == null) {
                // try global supplementary files
                template = Resources.file(dd.getWorkspaces(path));
            }

            if (template != null) {
                return new AutopopulateTemplate(template.getAbsolutePath());
            }

            if (resource.getStore() != null && resource.getStore().getWorkspace() != null) {
                // next try relative to the workspace
                template = Resources.file(dd.get(resource.getStore().getWorkspace(), path));

                if (template == null) {
                    // try global supplementary files
                    template = Resources.file(dd.getWorkspaces(path));
                }

                if (template != null) {
                    return new AutopopulateTemplate(template.getAbsolutePath());
                }
            }
        }

        return null;
    }
}
