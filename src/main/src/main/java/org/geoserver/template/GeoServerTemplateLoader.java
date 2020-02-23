/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resources;

/**
 * A freemarker template loader which can load templates from locations under a GeoServer data
 * directory.
 *
 * <p>To use this template loader, use the {@link Configuration#setTemplateLoader(TemplateLoader)}
 * method:
 *
 * <pre>
 *         <code>
 *  Configuration cfg = TemplateUtils.getSafeConfiguration();
 *  cfg.setTemplateLoader( new GeoServerTemplateLoader() );
 *  ...
 *  Template template = cfg.getTemplate( "foo.ftl" );
 *  ...
 *         </code>
 * </pre>
 *
 * <p>In {@link #findTemplateSource(String)}, the following lookup heuristic is applied to locate a
 * file based on the given path.
 *
 * <ol>
 *   <li>The path relative to '<data_dir>/featureTypes/[featureType]' given that a feature ( {@link
 *       #setFeatureType(String)} ) has been set
 *   <li>The path relative to '<data_dir>/featureTypes'
 *   <li>The path relative to '<data_dir>/templates'
 *   <li>The path relative to the calling class with {@link Class#getResource(String)}.
 * </ol>
 *
 * <b>Note:</b> If method 5 succeeds, the resulting template will be copied to the 'templates'
 * directory of the data directory.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GeoServerTemplateLoader implements TemplateLoader {
    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.template");

    /** Delegate file based template loader */
    FileTemplateLoader fileTemplateLoader;

    /** Delegate class based template loader, may be null depending on how */
    ClassTemplateLoader classTemplateLoader;

    /** GeoServer data directory */
    GeoServerDataDirectory dd;

    /**
     * Feature type directory to load template against. Its presence is mutually exclusive with
     * coverageName
     */
    protected ResourceInfo resource;

    /** Allows for workspace specific lookups */
    WorkspaceInfo workspace;

    /**
     * Constructs the template loader.
     *
     * @param caller The "calling" class, used to look up templates based with {@link
     *     Class#getResource(String)}, may be <code>null</code>
     * @param rl The geoserver resource loader
     */
    public GeoServerTemplateLoader(Class caller, GeoServerResourceLoader rl) throws IOException {
        this(
                caller,
                rl == null
                        ? new GeoServerDataDirectory(
                                GeoServerExtensions.bean(GeoServerResourceLoader.class))
                        : new GeoServerDataDirectory(rl));
    }

    public GeoServerTemplateLoader(Class caller, GeoServerDataDirectory dd) throws IOException {
        this.dd = dd;

        // create a file template loader to delegate to
        fileTemplateLoader = new FileTemplateLoader(dd.root());

        // create a class template loader to delegate to
        if (caller != null) {
            classTemplateLoader = new ClassTemplateLoader(caller, "");
        }
    }

    public void setFeatureType(FeatureTypeInfo ft) {
        this.resource = ft;
    }

    public void setWMSLayer(WMSLayerInfo wms) {
        this.resource = wms;
    }

    public void setWMTSLayer(WMTSLayerInfo wmts) {
        this.resource = wmts;
    }

    public void setCoverage(CoverageInfo c) {
        this.resource = c;
    }

    public void setResource(ResourceInfo resource) {
        this.resource = resource;
    }

    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    public Object findTemplateSource(String path) throws IOException {
        File template = null;

        // template look up order
        // 1. Relative to resource
        // 2. Relative to store of the resource
        // 3. Relative to workspace of resource
        // 4. Relative to workspaces directory
        // 5. Relative to templates directory
        // 6. Relative to the class

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
                return template;
            }
        }

        if (workspace != null) {
            if (template == null) {
                // next try relative to the workspace
                template = Resources.file(dd.get(workspace, path));
            }

            if (template == null) {
                // try global supplementary files
                template = Resources.file(dd.getWorkspaces(path));
            }

            if (template != null) {
                return template;
            }
        }

        // for backwards compatability, use the old lookup mechanism
        template = findTemplateSourceLegacy(path);
        if (template != null) {
            return template;
        }

        // next, check the templates directory
        template =
                (File) fileTemplateLoader.findTemplateSource("templates" + File.separator + path);

        if (template != null) {
            return template;
        }

        // final effort to use a class resource
        if (classTemplateLoader != null) {
            Object source = classTemplateLoader.findTemplateSource(path);

            // wrap the source in a source that maintains the orignial path
            if (source != null) {
                return new ClassTemplateSource(path, source);
            }
        }

        return null;
    }

    File findTemplateSourceLegacy(String path) throws IOException {
        File template = null;

        // first check relative to set feature type
        try {
            template =
                    (File)
                            fileTemplateLoader.findTemplateSource(
                                    "featureTypes" + File.separator + path);

            if (template != null) {
                return template;
            }

            // next, try relative to featureTypes or coverages directory, as appropriate
            template =
                    (File)
                            fileTemplateLoader.findTemplateSource(
                                    "coverages" + File.separator + path);

            if (template != null) {
                return template;
            }

        } catch (NoSuchElementException e) {
            // this one is thrown if the feature type is not found, and happens whenever
            // the feature type is a remote one
            // No problem, we just go on, there won't be any specific template for it
        }

        return null;
    }

    public long getLastModified(Object source) {
        if (source instanceof File) {
            // loaded from file
            return fileTemplateLoader.getLastModified(source);
        } else {
            // loaded from class
            ClassTemplateSource wrapper = (ClassTemplateSource) source;

            return classTemplateLoader.getLastModified(wrapper.source);
        }
    }

    public Reader getReader(Object source, String encoding) throws IOException {
        if (source instanceof File) {
            // loaded from file
            return fileTemplateLoader.getReader(source, encoding);
        } else {
            // get teh resource for the raw source as use it right away
            ClassTemplateSource wrapper = (ClassTemplateSource) source;

            return classTemplateLoader.getReader(wrapper.source, encoding);
        }
    }

    public void closeTemplateSource(Object source) throws IOException {
        if (source instanceof File) {
            fileTemplateLoader.closeTemplateSource(source);
        } else {
            ClassTemplateSource wrapper = (ClassTemplateSource) source;

            // close the raw source
            classTemplateLoader.closeTemplateSource(wrapper.source);

            // cleanup
            wrapper.path = null;
            wrapper.source = null;
        }
    }

    /**
     * Template source for use when a template is loaded from a class.
     *
     * <p>Used to store the intial path so the template can be copied to the data directory.
     *
     * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
     */
    protected static class ClassTemplateSource {
        /** The path used to load the template. */
        String path;

        /** The raw source from the class template loader */
        Object source;

        public ClassTemplateSource(String path, Object source) {
            this.path = path;
            this.source = source;
        }
    }
}
