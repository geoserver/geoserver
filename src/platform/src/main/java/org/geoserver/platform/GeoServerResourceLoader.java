/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;


/**
 * Manages resources in GeoServer.
 * <p>
 * The loader maintains a search path in which it will use to look up resources.
 * The {@link #baseDirectory} is a member of this path.
 * </p>
 * <p>
 * Files and directories created by the resource loader are made relative to
 * {@link #baseDirectory}.
 * </p>
 * <p>
 * <pre>
 *         <code>
 * File dataDirectory = ...
 * GeoServerResourceLoader loader = new GeoServerResourceLoader( dataDirectory );
 * loader.addSearchLocation( new File( "/WEB-INF/" ) );
 * loader.addSearchLocation( new File( "/data" ) );
 * ...
 * File catalog = loader.find( "catalog.xml" );
 *         </code>
 * </pre>
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class GeoServerResourceLoader extends DefaultResourceLoader implements ApplicationContextAware {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global");

    /** "path" for resource lookups */
    Set searchLocations;

    /**
     * Base directory
     */
    File baseDirectory;

    /**
     * Creates a new resource loader with no base directory.
     * <p>
     * Such a constructed resource loader is not capable of creating resources
     * from relative paths.
     * </p>
     */
    public GeoServerResourceLoader() {
        searchLocations = new TreeSet();
    }

    /**
     * Creates a new resource loader.
     *
     * @param baseDirectory The directory in which
     */
    public GeoServerResourceLoader(File baseDirectory) {
        this();
        this.baseDirectory = baseDirectory;
        setSearchLocations(Collections.EMPTY_SET);
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (baseDirectory == null) {
            //lookup the data directory
            if (applicationContext instanceof WebApplicationContext) {
                String data = lookupGeoServerDataDirectory(
                        ((WebApplicationContext)applicationContext).getServletContext());
                if (data != null) {
                    setBaseDirectory(new File(data)); 
                }
            }
        }
        
        //add additional lookup locations
        if (baseDirectory != null) {
            addSearchLocation(new File(baseDirectory, "data"));
        }

        if (applicationContext instanceof WebApplicationContext) {
            ServletContext servletContext = 
                ((WebApplicationContext)applicationContext).getServletContext();
            if (servletContext != null) {
                String path = servletContext.getRealPath("WEB-INF");
                if (path != null) {
                    addSearchLocation(new File(path));
                }
                path = servletContext.getRealPath("/");
                if (path != null) {
                    addSearchLocation(new File(path));
                }
            }
        }
    }
    
    /**
     * Adds a location to the path used for resource lookups.
     *
     * @param A directory containing resources.
     */
    public void addSearchLocation(File searchLocation) {
        searchLocations.add(searchLocation);
    }

    /**
     * Sets the search locations used for resource lookups.
     *
     * @param searchLocations A set of {@link File}.
     */
    public void setSearchLocations(Set searchLocations) {
        this.searchLocations = new HashSet(searchLocations);

        //always add the base directory
        if (baseDirectory != null) {
            this.searchLocations.add(baseDirectory);
        }
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
     * @param baseDirectory
     */
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;

        searchLocations.add(baseDirectory);
    }

    /**
     * Performs a resource lookup.
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
        return find( null, location );
    }
    
    /**
     * Performs a resource lookup, optionally specifying the containing directory.
     *
     * @param parent The containing directory, optionally null. 
     * @param location The name of the resource to lookup, can be absolute or
     * relative.
     *
     * @return The file handle representing the resource, or null if the
     * resource could not be found.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File find(File parent, String location) throws IOException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Looking up resource " + location + " with parent " 
                + (parent != null ? parent.getPath() : "null"));
        }
        
        //first to an existance check
        File file = parent != null ? new File(parent,location) : new File(location);
        
        if (parent != null && file.exists()) {
            return file;
        }
        
        if (file.isAbsolute()) {
            return file.exists() ? file : null;
        } else {
            // try relative to base dir
            String path = file.getPath();
            file = new File(baseDirectory, path);
            if (file.exists()) {
                return file;
            }
            for (Iterator f = searchLocations.iterator(); f.hasNext();) {
                File base = (File) f.next();
                file = new File(base, path);

                try {
                    if (file.exists()) {
                        return file;
                    }
                } catch (SecurityException e) {
                    LOGGER.warning("Failed attemp to check existance of " + file.getAbsolutePath());
                }
            }
        }

        //look for a generic resource if no parent specified
        if ( parent == null ) {
            Resource resource = getResource(location);
    
            if (resource.exists()) {
                return resource.getFile();
            }
        }

        return null;
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
        return find( null, location );
    }

    /**
     * Performs a resource lookup, optionally specifying a containing directory.
     * <p>
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre> 
     * </p>
     * @param parent The parent directory, may be null.
     * @param location The components of the path of the resource to lookup.
     * 
     * @return The file handle representing the resource, or null if the
     *  resource could not be found.
     *  
     * @throws IOException Any I/O errors that occur.
     */
    public File find( File parent, String... location ) throws IOException {
        return find( parent, concat( location ) );
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
        return findOrCreateDirectory(null,location);
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parent The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     */
    public File findOrCreateDirectory( File parent, String... location ) throws IOException {
        return findOrCreateDirectory(parent, concat(location));
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
        return findOrCreateDirectory(null,location);
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parent The containing directory, may be null.
     * @param location The location of the directory to find or create.
     * 
     * @return The file handle.
     * 
     * @throws IOException If any i/o errors occur.
     */
    public File findOrCreateDirectory( File parent, String location ) throws IOException {
        File dir = find( parent, location );
        if ( dir != null ) {
            if ( !dir.isDirectory() ) {
                //location exists, but is a file
                throw new IllegalArgumentException( "Location '" + location + "' specifies a file");
            }
            
            return dir;
        }
        
        //create it
        return createDirectory( parent, location );
    }
    
    /**
     * Creates a new directory specifying components of the location.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     */
    public File createDirectory(String... location) throws IOException {
        return createDirectory(null,location);
    }
    
    /**
     * Creates a new directory specifying components of the location, and the containing directory.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     */
    public File createDirectory(File parent, String... location) throws IOException {
        return createDirectory(parent,concat(location));
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
        return createDirectory(null,location);
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
    public File createDirectory(File parent, String location) throws IOException {
        File file = find(parent,location);

        if (file != null) {
            if (!file.isDirectory()) {
                String msg = location + " already exists and is not directory";
                throw new IOException(msg);
            }
        }
        
        file = parent != null ? new File(parent,location) : new File(location);

        if (file.isAbsolute()) {
            file.mkdirs();

            return file;
        }

        // no base directory set, cannot create a relative path
        if (baseDirectory == null) {
             String msg = "No base location set, could not create directory: " + location;
             throw new IOException(msg);
        }

        if (parent != null && parent.getPath().startsWith(baseDirectory.getPath())) {
            //parent contains base directory path, make relative to it
            file = new File(parent, location);
        }
        else {
            //base relative to base directory
            file = parent != null ? new File(new File(baseDirectory, parent.getPath()), location)
                : new File(baseDirectory, location);
        }

        file.mkdirs();
        return file;
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
        return createFile( concat(location) );
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
        return createFile(null,location);
    }
    
    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(File, String)}
     * </p>
     * @param location Location of file to create, either absolute or relative.
     * @param parent The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parent, String... location) throws IOException{
        return createFile(parent,concat(location));
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
     * @param parent The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parent, String location) throws IOException{
        File file = find(parent,location);

        if (file != null) {
            if (file.isDirectory()) {
                String msg = location + " already exists and is a directory";
                throw new IOException(msg);
            }

            return file;
        }
        
        file = parent != null ? new File(parent,location) : new File(location);

        if (parent == null) {
            // no base directory set, cannot create a relative path
            if (baseDirectory == null) {
                String msg = "No base location set, could not create file: " + file.getPath();
                throw new IOException(msg);
            }

            file = new File(baseDirectory, file.getPath());
        }

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        
        return file;
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
     * @param to The destination to copy to.
     */
    public void copyFromClassPath( String resource, String to ) throws IOException {
        File target = new File( to );
        if ( !target.isAbsolute() ) {
            target = new File( getBaseDirectory(), to );
        }
        
        copyFromClassPath(resource, target);
    }
    
    /**
     * Copies a resource from the classpath to a specified file.
     * 
     */
    public void copyFromClassPath( String resource, File target ) throws IOException {
        copyFromClassPath( resource, target, null );
    }
    
    /**
     * Copies a resource relative to a particular class from the classpath to the specified file. 
     */
    public void copyFromClassPath( String resource, File target, Class scope ) throws IOException {
        InputStream is = null; 
        OutputStream os = null;
        byte[] buffer = new byte[4096];
        int read;

        try{
            // Get the resource
            if (scope == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);    
                if(is==null) {
                    throw new IOException("Could not load " + resource + " from scope "+
                            Thread.currentThread().getContextClassLoader().toString()+".");
                }
            } else {
                is = scope.getResourceAsStream(resource);
                if(is==null) {
                    throw new IOException("Could not load " + resource + " from scope "+
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
        // Loop over variable names
        for (int i = 0; i < varStrs.length && dataDirStr == null; i++) {
            
            // Loop over variable access methods
            for (int j = 0; j < typeStrs.length && dataDirStr == null; j++) {
                String value = null;
                String varStr = new String(varStrs[i]);
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
                if(verifyDataDir(typeStr, varStr, fh)) {
                    // Sweet, we can work with this
                    dataDirStr = value;
                } 
            }
        }
        
        // fall back to embedded data dir
        if(dataDirStr == null) {
            dataDirStr = servContext.getRealPath("/data");
            if(dataDirStr == null) {
                throw new RuntimeException("The embedded data dir is not available, GeoServer cannot start!");
            } else if(!verifyDataDir("default embedded data dir", "", new File(dataDirStr))) {
                throw new RuntimeException("The embedded data dir is available but not usable (see previous messages), GeoServer cannot start!");
            } else {
                LOGGER.info("Falling back to embedded data directory: " + dataDirStr);
            }
        }
        
        return dataDirStr;
    }
    
    private static boolean verifyDataDir(String typeStr, String varStr, File fh) {
        // Being a bit pessimistic here
        String msgPrefix = "Found " + typeStr + varStr + " set to " + fh.getPath();
        if (!fh.exists()) {
            LOGGER.warning(msgPrefix + " , but this path does not exist");
            return false;
        }
        if (!fh.isDirectory()) {
            LOGGER.warning(msgPrefix + " , which is not a directory");
            return false;
        }
        if (!fh.canWrite()) {
            LOGGER.warning(msgPrefix + " , which is not writeable");
            return false;
        }
        
        return true;
    }
}
