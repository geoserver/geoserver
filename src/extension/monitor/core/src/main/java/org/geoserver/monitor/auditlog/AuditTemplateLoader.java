/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.auditlog;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.GeoServerTemplateLoader;

/**
 * Similar to the {@link GeoServerTemplateLoader}, but does not work relative to the resource
 * directories
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AuditTemplateLoader implements TemplateLoader {

    /** Delegate file based template loader */
    FileTemplateLoader fileTemplateLoader;

    /** Delegate class based template loader, may be null depending on how */
    ClassTemplateLoader classTemplateLoader;

    public AuditTemplateLoader(GeoServerResourceLoader rl) throws IOException {
        // the two delegate loaders
        fileTemplateLoader = new FileTemplateLoader(rl.getBaseDirectory());
        classTemplateLoader = new ClassTemplateLoader(getClass(), "");
    }

    @Override
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

    @Override
    public Object findTemplateSource(String path) throws IOException {
        Object result = fileTemplateLoader.findTemplateSource("monitoring/" + path);
        if (result != null) {
            return result;
        } else {
            Object source = classTemplateLoader.findTemplateSource(path);

            // wrap the source in a source that maintains the orignial path
            if (source != null) {
                return new ClassTemplateSource(path, source);
            }
        }
        return null;
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

    @Override
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

    /**
     * Template source for use when a template is loaded from a class.
     *
     * <p>Used to store the intial path so the template can be copied to the data directory.
     *
     * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
     */
    static class ClassTemplateSource {
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
