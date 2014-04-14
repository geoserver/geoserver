/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * Access to resources in GeoServer including configuration information and unmanaged cache or log files.
 * <p>
 * The loader maintains a search path in which it will use to look up resources.
 * <ul>
 * <li>Configuration is accessed using {@link ResourceStore#get(String)} which provides stream based access. If required configuration can be unpacked
 * into a file in the data directory. The most common example is for use as a template.
 * <li>Files in the data directory can also be used as a temporary cache. These files should be considered temporary and may need to be recreated
 * (when upgrading or for use on different nodes in a cluster).</li>
 * <li>
 * </ul>
 * The {@link #baseDirectory} is a member of this path. Files and directories created by the resource loader are made relative to
 * {@link #baseDirectory}.
 * </p>
 * <p>
 * 
 * <pre>
 * <code>
 * File dataDirectory = ...
 * GeoServerResourceLoader loader = new GeoServerResourceLoader( dataDirectory );
 * ...
 * Resource catalog = loader.get("catalog.xml");
 * File log = loader.find("logs/geoserver.log");
 * </code>
 * </pre>
 * 
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * 
 */
public class GeoServerResourceLoader extends DefaultResourceLoader implements ResourceStore {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global");
    static {
        LOGGER.setLevel(Level.FINER);
    }
    
    /**
     * ResourceStore used for configuration resources.
     * 
     * Initially this is configured to access resources in the base directory, however spring may inject an external implementation (jdbc database
     * blob, github, ...).
     */
    ResourceStore resources;

    /**
     * Base directory used to access unmanaged files.
     */
    File baseDirectory;

    /**
     * Creates a new resource loader (with no base directory).
     * <p>
     * Used to construct a GeoServerResourceLoader for test cases (and is unable to create resources from relative paths.
     * </p>
     */
    public GeoServerResourceLoader() {
        baseDirectory = null;
        resources = ResourceStore.EMPTY;
    }

    /**
     * Creates a new resource loader.
     *
     * @param baseDirectory The directory in which
     */
    public GeoServerResourceLoader(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.resources = new FileSystemResourceStore( baseDirectory );
    }
    
    /**
     * Creates a new resource loader.
     *
     * @param baseDirectory The directory in which
     */
    public GeoServerResourceLoader(ResourceStore resourceStore) {
        this.baseDirectory = resourceStore.get(Paths.BASE).dir();
        this.resources = resourceStore;
    }
    
    
// public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        if (baseDirectory == null) {
//            //lookup the data directory
//            if (applicationContext instanceof WebApplicationContext) {
//                String data = lookupGeoServerDataDirectory(
//                        ((WebApplicationContext)applicationContext).getServletContext());
//                if (data != null) {
//                    setBaseDirectory(new File(data)); 
//                }
//            }
//        }
//        if( resources == Resources.EMPTY ){
//            // lookup the configuration resources
//            if( baseDirectory != null ){
//                resources = new FileSystemResourceStore( baseDirectory );
//            }
//        }
//    }
    
    /**
     * Adds a location to the path used for resource lookups.
     *
     * @param A directory containing resources.
     * @deprecated No longert used
     */
    public void addSearchLocation(File searchLocation) {
        //searchLocations.add(searchLocation);
    }

    /**
     * Sets the search locations used for resource lookups.
     * 
     * The {@link #baseDirectory} is always incuded in {@link #searchLocations}.
     *
     * @param searchLocations A set of {@link File}.
     * @deprecated No longer used
     */
    public void setSearchLocations(Set<File> searchLocations) {
    }

    /**
     * @return The base directory.
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Sets the base directory.
     * 
     * The base directory is included in {@link #searchLocations}.
     *
     * @param baseDirectory
     */
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.resources = new FileSystemResourceStore( baseDirectory );
    }

    @Override
    public Resource get(String path) {
        return resources.get(path);
    }
    @Override
    public boolean move(String path, String target) {
        return resources.move(path, target);
    }
    @Override
    public boolean remove(String path) {
        return resources.remove( path );
    }
    
    /**
     * Performs file lookup.
     *
     * @param location The name of the resource to lookup, can be absolute or
     * relative.
     *
     * @return The file handle representing the resource, or null if the
     * resource could not be found.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File find( String location ) throws IOException {
        Resource resource = get( Paths.convert(location) );
        return Resources.find( resource );
    }
    
    /**
     * Performs a resource lookup, optionally specifying the containing directory.
     *
     * @param parentFile The containing directory, optionally null. 
     * @param location The name of the resource to lookup, can be absolute or
     * relative.
     *
     * @return The file handle representing the resource, or null if the
     * resource could not be found.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File find(File parentFile, String location) throws IOException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Looking up resource " + location + " with parent " 
                + (parentFile != null ? parentFile.getPath() : "null"));
        }
        Resource resource = get( Paths.convert(getBaseDirectory(), parentFile, location ));
        return Resources.find( resource );
    }

    
    /**
     * Performs a resource lookup.
     * <p>
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre> 
     * </p>
     * @param location The components of the path of the resource to lookup.
     * 
     * @return The file handle representing the resource, or null if the
     *  resource could not be found.
     *  
     * @throws IOException Any I/O errors that occur.
     */
    public File find( String... location ) throws IOException {        
        Resource resource = get( Paths.path(location) );
        return Resources.find( resource );
    }

    /**
     * Performs a resource lookup, optionally specifying a containing directory.
     * <p>
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre> 
     * </p>
     * @param parentFile The parent directory, may be null.
     * @param location The components of the path of the resource to lookup.
     * 
     * @return The file handle representing the resource, or null if the
     *  resource could not be found.
     *  
     * @throws IOException Any I/O errors that occur.
     */
    public File find( File parentFile, String... location ) throws IOException {
        Resource resource = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.find(resource);
    }

    /**
     * Helper method to build up a file path from components.
     */
    String concat( String... location ) {
        StringBuffer loc = new StringBuffer();
        for ( int i = 0; i < location.length; i++ ) {
            loc.append( location[i] ).append( File.separator );
        }
        loc.setLength(loc.length()-1);
        return loc.toString();
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     */
    public File findOrCreateDirectory( String... location ) throws IOException {
        Resource directory = get( Paths.path(location) );        
        return directory.dir(); // will create directory as needed
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parentFile The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     */
    public File findOrCreateDirectory( File parentFile, String... location ) throws IOException {
        Resource directory = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return directory.dir(); // will create directory as needed
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param location The location of the directory to find or create.
     * 
     * @return The file handle.
     * 
     * @throws IOException If any i/o errors occur.
     */
    public File findOrCreateDirectory( String location ) throws IOException {
        Resource directory = get( Paths.convert(location) );        
        return directory.dir(); // will create directory as needed
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parentFile The containing directory, may be null.
     * @param location The location of the directory to find or create.
     * 
     * @return The file handle.
     * 
     * @throws IOException If any i/o errors occur.
     */
    public File findOrCreateDirectory( File parentFile, String location ) throws IOException {
        Resource directory = get( Paths.convert(getBaseDirectory(),parentFile,location) );
        return directory.dir(); // will create directory as needed
    }
    
    /**
     * Creates a new directory specifying components of the location.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     */
    public File createDirectory(String... location) throws IOException {
        Resource directory = get( Paths.path(location) );
        return Resources.createNewDirectory(directory);
    }
    
    /**
     * Creates a new directory specifying components of the location, and the containing directory.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     * @param parentFile The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to create
     * @return newly created directory
     */
    public File createDirectory(File parentFile, String... location) throws IOException {
        Resource directory = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.createNewDirectory(directory);
    }
    
    /**
     * Creates a new directory.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a file, an IOException is thrown.
     * </p>
     * @param location Location of directory to create, either absolute or
     * relative.
     *
     * @return The file handle of the created directory.
     *
     * @throws IOException
     */
    public File createDirectory(String location) throws IOException {
        Resource directory = get( Paths.convert(location) );
        return Resources.createNewDirectory(directory);
    }
    
    /**
     * Creates a new directory, optionally specifying a containing directory.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a file, an IOException is thrown.
     * </p>
     * @param parent The containing directory, may be null.
     * @param location Location of directory to create, either absolute or
     * relative.
     *
     * @return The file handle of the created directory.
     *
     * @throws IOException
     */
    public File createDirectory(File parentFile, String location) throws IOException {
        Resource directory = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.createNewDirectory(directory);
    }

    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(String)}.
     * </p>
     * 
     * @param location The components of the location.
     *
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(String ...location) throws IOException {
        Resource resource = get( Paths.path(location) );
        return Resources.createNewFile( resource );
    }
    
    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(File, String)}
     * </p>
     * @param location Location of file to create, either absolute or relative.
     *
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(String location) throws IOException {
        Resource resource = get( Paths.convert(location) );
        return Resources.createNewFile( resource );
    }
    
    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(File, String)}
     * </p>
     * @param location Location of file to create, either absolute or relative.
     * @param parentFile The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parentFile, String... location) throws IOException{
        Resource resource = get( Paths.convert(getBaseDirectory(), parentFile, location ));
        return Resources.createNewFile(resource);
    }
    
    /**
     * Creates a new file.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * </p>
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a directory, an IOException is thrown.
     * </p>
     * @param location Location of file to create, either absolute or relative.
     * @param parentFile The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parentFile, String location) throws IOException{
        Resource resource = get( Paths.convert(getBaseDirectory(), parentFile, location ));
        return Resources.createNewFile(resource);
    }
    
    /**
     * Copies a resource located on the classpath to a specified path.
     * <p>
     * The <tt>resource</tt> is obtained from teh context class loader of the 
     * current thread. When the <tt>to</tt> parameter is specified as a relative
     * path it is considered to be relative to {@link #getBaseDirectory()}.
      </p>
     * 
     * @param resource The resource to copy.
     * @param location The destination to copy to.
     */
    public void copyFromClassPath( String classpathResource, String location ) throws IOException {
        Resource resource = get(Paths.convert(location));
        copyFromClassPath( classpathResource, resource.file() );
    }
    
    /**
     * Copies a resource from the classpath to a specified file.
     * 
     * @param classpathResource Path to classpath content to be copied
     * @param target File to copy content into (must be already created)
     */
    public void copyFromClassPath( String classpathResource, File target ) throws IOException {
        copyFromClassPath( classpathResource, target, null );
    }
    
    /**
     * Copies a resource relative to a particular class from the classpath to the specified file. 
     * 
     * @param classpathResource Path to classpath content to be copied
     * @param target File to copy content into (must be already created)
     * @param scope Class used as base for classpathResource
     */
    
    public void copyFromClassPath( String classpathResource, File target, Class<?> scope ) throws IOException {
        InputStream is = null; 
        OutputStream os = null;
        byte[] buffer = new byte[4096];
        int read;

        try{
            // Get the resource
            if (scope == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource);    
                if(is==null) {
                    throw new IOException("Could not load " + classpathResource + " from scope "+
                            Thread.currentThread().getContextClassLoader().toString()+".");
                }
            } else {
                is = scope.getResourceAsStream(classpathResource);
                if(is==null) {
                    throw new IOException("Could not load " + classpathResource + " from scope "+
                                 scope.toString()+".");
                }
            }
    
            // Write it to the target
            try {
                os = new FileOutputStream(target);
                while((read = is.read(buffer)) > 0)
                    os.write(buffer, 0, read);
            } catch (FileNotFoundException targetException) {
                throw new IOException("Can't write to file " + target.getAbsolutePath() + 
                        ". Check write permissions on target folder for user " + System.getProperty("user.name"));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error trying to copy logging configuration file", e);
            }
        } finally {
            // Clean up
            try {
                if(is != null){
                    is.close();
                }
                if(os != null){
                    os.close();
                }
            } catch(IOException e) {
                // we tried...
            }
        }
    }
    
    /**
     * Determines the location of the geoserver data directory based on the following lookup
     * mechanism:
     *  
     * 1) Java environment variable
     * 2) Servlet context variable
     * 3) System variable 
     *
     * For each of these, the methods checks that
     * 1) The path exists
     * 2) Is a directory
     * 3) Is writable
     * 
     * @param servContext The servlet context.
     * @return String The absolute path to the data directory, or <code>null</code> if it could not
     * be found. 
     */
    public static String lookupGeoServerDataDirectory(ServletContext servContext) {
        
        final String[] typeStrs = { "Java environment variable ",
                "Servlet context parameter ", "System environment variable " };

        final String[] varStrs = { "GEOSERVER_DATA_DIR", "GEOSERVER_DATA_ROOT" };

        String dataDirStr = null;
        String msgPrefix = null;
        
        // Loop over variable names
        for (int i = 0; i < varStrs.length && dataDirStr == null; i++) {
            
            // Loop over variable access methods
            for (int j = 0; j < typeStrs.length && dataDirStr == null; j++) {
                String value = null;
                String varStr = varStrs[i];
                String typeStr = typeStrs[j];

                // Lookup section
                switch (j) {
                case 0:
                    value = System.getProperty(varStr);
                    break;
                case 1:
                    value = servContext.getInitParameter(varStr);
                    break;
                case 2:
                    value = System.getenv(varStr);
                    break;
                }

                if (value == null || value.equalsIgnoreCase("")) {
                    LOGGER.finer("Found " + typeStr + varStr + " to be unset");
                    continue;
                }

                
                // Verify section
                File fh = new File(value);

                // Being a bit pessimistic here
                msgPrefix = "Found " + typeStr + varStr + " set to " + value;

                if (!fh.exists()) {
                    LOGGER.warning(msgPrefix + " , but this path does not exist");
                    continue;
                }
                if (!fh.isDirectory()) {
                    LOGGER.warning(msgPrefix + " , which is not a directory");
                    continue;
                }
                if (!fh.canWrite()) {
                    LOGGER.warning(msgPrefix + " , which is not writeable");
                    continue;
                }

                // Sweet, we can work with this
                dataDirStr = value;
            }
        }
        
        // fall back to embedded data dir
        if(dataDirStr == null) {
            dataDirStr = servContext.getRealPath("/data");
            LOGGER.info("Falling back to embedded data directory: " + dataDirStr);
        }
        
        return dataDirStr;
    }

}
