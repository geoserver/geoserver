/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.opengis.feature.simple.SimpleFeatureType;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;


/**
 * A freemarker template loader which can load templates from locations under
 * a GeoServer data directory.
 * <p>
 * To use this template loader, use the {@link Configuration#setTemplateLoader(TemplateLoader)}
 * method:
 * <pre>
 *         <code>
 *  Configuration cfg = new Configuration();
 *  cfg.setTemplateLoader( new GeoServerTemplateLoader() );
 *  ...
 *  Template template = cfg.getTemplate( "foo.ftl" );
 *  ...
 *         </code>
 * </pre>
 * </p>
 * <p>
 * In {@link #findTemplateSource(String)}, the following lookup heuristic is
 * applied to locate a file based on the given path.
 * <ol>
 *  <li>The path relative to '<data_dir>/featureTypes/[featureType]'
 *          given that a feature ( {@link #setFeatureType(String)} ) has been set
 *  <li>The path relative to '<data_dir>/featureTypes'
 *  <li>The path relative to '<data_dir>/templates'
 *  <li>The path relative to the calling class with {@link Class#getResource(String)}.
 * </ol>
 * <b>Note:</b> If method 5 succeeds, the resulting template will be copied to
 * the 'templates' directory of the data directory.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class GeoServerTemplateLoader implements TemplateLoader {
    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.template");

    /**
     * Delegate file based template loader
     */
    FileTemplateLoader fileTemplateLoader;

    /**
     * Delegate class based template loader, may be null depending on how
     */
    ClassTemplateLoader classTemplateLoader;
    
    /**
     * GeoServer data directory 
     */
    GeoServerDataDirectory dd;
    
    /**
     * Feature type directory to load template against. Its presence is mutually
     * exclusive with coverageName
     */
    protected ResourceInfo resource;
    
    /**
     * Feature type directory to load template against. Its presence is mutually
     * exclusive with coverageName
     * @deprecated Keeping this around for backwards compatibility, use resource
     */
    SimpleFeatureType featureType;

    /**
     * Coverage info directory to load template against. Its presence is mutually
     * exclusive with featureTypeInfo
     * @deprecated Keeping this around for backwards compatibility, use resource
     */
    private String coverageName;

    /**
     * Reference to the GeoServer catalog so we can look up the prefix for a namespace.
     */
    private Catalog catalog;

    /**
     * Constructs the template loader.
     *
     * @param caller The "calling" class, used to look up templates based with
     * {@link Class#getResource(String)}, may be <code>null</code>
     *
     * @deprecated Use {@link #GeoServerTemplateLoader(Class, GeoServerResourceLoader)}  
     * @throws IOException
     */
    public GeoServerTemplateLoader(Class caller) throws IOException {
        this(caller,GeoServerExtensions.bean(GeoServerResourceLoader.class));
    }
    
    /**
     * Constructs the template loader.
     *
     * @param caller The "calling" class, used to look up templates based with
     * {@link Class#getResource(String)}, may be <code>null</code>
     * @param rl The geoserver resource loader
     *
     * @throws IOException
     */
    public GeoServerTemplateLoader(Class caller, GeoServerResourceLoader rl) throws IOException {
        this(caller, new GeoServerDataDirectory(rl));
    }

    public GeoServerTemplateLoader(Class caller, GeoServerDataDirectory dd) throws IOException {
        this.dd = dd;

        //create a file template loader to delegate to
        fileTemplateLoader = new FileTemplateLoader(dd.root());

        //grab the catalog and store a reference
        catalog = (Catalog)GeoServerExtensions.bean("catalog");

        //create a class template loader to delegate to
        if (caller != null) {
            classTemplateLoader = new ClassTemplateLoader(caller, "");
        }
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Sets the feature type in which templates are loaded against.
     * @deprecated use {@link #setFeatureType(FeatureTypeInfo)}
     */
    public void setFeatureType(SimpleFeatureType featureType) {
        this.featureType = featureType;
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( featureType.getName() );
        if ( ft == null ) {
            return;
            //throw new IllegalArgumentException("No feature type named " + featureType.getName() + " in catalog");
        }
        
        setFeatureType(ft);
    }
    
    public void setFeatureType(FeatureTypeInfo ft) {
        this.resource = ft;
    }

    public void setWMSLayer(WMSLayerInfo wms){
        this.resource = wms;
    }

    public void setWMTSLayer(WMTSLayerInfo wmts){
        this.resource = wmts;
    }
    
    /**
     * Sets the coverage info
     * @deprecated use {@link #setCoverage(CoverageInfo)}
     */
    public void setCoverageName(String coverageName){
        this.coverageName = coverageName;
        CoverageInfo c = catalog.getCoverageByName( coverageName );
        if ( c == null ) {
            return;
            //throw new IllegalArgumentException("No coverage named " + coverageName + " in catalog");
        }
        setCoverage(c);
    }
    
    public void setCoverage(CoverageInfo c) {
        this.resource = c;
    }
    
    public void setResource(ResourceInfo resource){
        this.resource = resource;
    }
    
    public Object findTemplateSource(String path) throws IOException {
        File template = null;

        //template look up order
        // 1. Relative to resource
        // 2. Relative to store of the resource
        // 3. Relative to workspace of resource
        // 4. Relative to workspaces directory
        // 5. Relative to templates directory
        // 6. Relative to the class
        
        if ( resource != null ) {
            //first check relative to set resource
            template = dd.findSuppResourceFile( resource, path);
            
            if ( template == null ) {
              //next try relative to the store
                template = dd.findSuppStoreFile( resource.getStore(), path);
            }
            
            if ( template == null ) {
                //next try relative to the workspace
                template = dd.findSuppWorkspaceFile( resource.getStore().getWorkspace(), path);
            }
            
            if ( template == null) {
                // try global supplementary files
                template = dd.findSuppWorkspacesFile( resource.getStore().getWorkspace(), path);
            }

            if ( template != null ) {
                return template;
            }
        }
        
        //for backwards compatability, use the old lookup mechanism
        template = findTemplateSourceLegacy(path);
        if ( template != null ) {
            return template;
        }
        
        //next, check the templates directory
        template = (File) fileTemplateLoader.findTemplateSource("templates" + File.separator + path);

        if (template != null) {
            return template;
        }

        //final effort to use a class resource
        if (classTemplateLoader != null) {
            Object source = classTemplateLoader.findTemplateSource(path);

            //wrap the source in a source that maintains the orignial path
            if (source != null) {
                return new ClassTemplateSource(path, source);
            }
        }

        return null;
    }
    
    File findTemplateSourceLegacy(String path) throws IOException {
        File template = null;
        
        //first check relative to set feature type
        String baseDirName;
        try {
            final String dirName;
            if (featureType != null) {
                baseDirName = "featureTypes";
                String name = featureType.getTypeName();
                String namespace = featureType.getName().getNamespaceURI();
                FeatureTypeInfo ftInfo = null;
                if(catalog != null && namespace != null) {
                    NamespaceInfo nsInfo = catalog.getNamespaceByURI(namespace);
                    if(nsInfo != null){
                        ftInfo = catalog.getFeatureTypeByName( nsInfo.getPrefix(), name);
                    }
                }
                if(catalog != null && ftInfo == null){ 
                    ftInfo = catalog.getFeatureTypeByName(name);
                }
                if(ftInfo != null){
                    String metadata = ftInfo.getMetadata().get("dirName",String.class);
                    if( metadata != null ){
                        dirName = metadata;
                    }
                    else {
                        dirName = ftInfo.getNamespace().getPrefix() + "_" + ftInfo.getName();
                    }
                }
                else {
                    dirName = null; // unavaialble
                }
            } else if (coverageName != null) {
                baseDirName = "coverages";
                CoverageInfo coverageInfo = catalog.getCoverageByName(coverageName);
                dirName = coverageInfo.getMetadata().get( "dirName", String.class );
            } else {
                baseDirName = "featureTypes";
                dirName = "";
            }
            
            template = (File) fileTemplateLoader.findTemplateSource(baseDirName + File.separator
                    + dirName + File.separator + path);

            if (template != null) {
                return template;
            }

            if (featureType != null) {
                NamespaceInfo nsInfo = null;
                if ( featureType.getName().getNamespaceURI() != null ) {
                    nsInfo = catalog.getNamespaceByURI(featureType.getName().getNamespaceURI());
                }
                // the feature type might not be registered, it may come from WMS feature portrayal, be a 
                // remote one
                if(nsInfo != null) {
                    //try looking up the template in the default location for the particular namespaces
                    // under templates/<namespace>
                    template = (File) fileTemplateLoader.findTemplateSource(
                            "templates" + File.separator + nsInfo.getPrefix() + File.separator + path
                            );
                }
            }

            if (template != null) return template;

            // next, try relative to featureTypes or coverages directory, as appropriate
            template = (File) fileTemplateLoader.findTemplateSource(baseDirName + File.separator
                    + path);

            if (template != null) {
                return template;
            }

        } catch(NoSuchElementException e) {
            // this one is thrown if the feature type is not found, and happens whenever
            // the feature type is a remote one
            // No problem, we just go on, there won't be any specific template for it
        }
        
        return null;
    }

    public long getLastModified(Object source) {
        if (source instanceof File) {
            //loaded from file
            return fileTemplateLoader.getLastModified(source);
        } else {
            //loaded from class
            ClassTemplateSource wrapper = (ClassTemplateSource) source;

            return classTemplateLoader.getLastModified(wrapper.source);
        }
    }

    public Reader getReader(Object source, String encoding)
        throws IOException {
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

            //close the raw source
            classTemplateLoader.closeTemplateSource(wrapper.source);

            //cleanup
            wrapper.path = null;
            wrapper.source = null;
        }
    }

    /**
     * Template source for use when a template is loaded from a class.
     * <p>
     * Used to store the intial path so the template can be copied to the data
     * directory.
     * </p>
     * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
     *
     */
    static class ClassTemplateSource {
        /**
         * The path used to load the template.
         */
        String path;

        /**
         * The raw source from the class template loader
         */
        Object source;

        public ClassTemplateSource(String path, Object source) {
            this.path = path;
            this.source = source;
        }
    }
}
