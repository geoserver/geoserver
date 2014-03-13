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
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
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
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("org/geoserver/platform/CorePlatform");
    /** "path" for resource lookups */
    private Set searchLocations;

    /**
     * Base directory
     */
    private File baseDirectory;
    
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
    public GeoServerResourceLoader(final File baseDirectory) {
        this();
        this.baseDirectory = baseDirectory;
        setSearchLocations(Collections.EMPTY_SET);
    }
    
    @Override
    public final void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
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
           final ServletContext servletContext = 
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
    public final void addSearchLocation(final File searchLocation) {
        searchLocations.add(searchLocation);
    }

    /**
     * Sets the search locations used for resource lookups.
     *
     * @param searchLocations A set of {@link File}.
     */
    public final void setSearchLocations(final Set searchLocations) {
        this.searchLocations = new HashSet(searchLocations);

        //always add the base directory
        if (baseDirectory != null) {
            this.searchLocations.add(baseDirectory);
        }
    }

    /**
     * @return The base directory.
     */
    public final File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Sets the base directory.
     *
     * @param baseDirectory
     */
    public final void setBaseDirectory(final File baseDirectory) {
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
    public final File find(final String location ) throws IOException {
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
    public final File find(final File parent, final String location) throws IOException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            final String msg = java.text.MessageFormat.format(
                    RESOURCE_BUNDLE.getString("LOOKING_UP_RESOURCE"), location, parent != null ? parent.getPath() : "null");
            LOGGER.log(Level.FINEST, msg);
        }
        
        //first to an existance check
        File file = parent != null ? new File(parent,location) : new File(location);
        
        if (file.exists()) {
            return file;
        }
        
        if (file.isAbsolute()) {
            return file.exists() ? file : null;
        } else {
            //try a relative url if no parent specified
            if ( parent == null ) {
                for (final Iterator f = searchLocations.iterator(); f.hasNext();) {
                    final File base = (File) f.next();
                    file = new File(base, location);
    
                    try {
                        if (file.exists()) {
                            return file;
                        }
                    } catch (SecurityException e) {
                        final String msg = java.text.MessageFormat.format(
                                RESOURCE_BUNDLE.getString("FAILED_ATTEMP_TO_CHECK_EXISTANCE"), file.getAbsolutePath(), e.getMessage());
                        LOGGER.log(Level.WARNING, msg, e);
                    }
                }
            }
            else {
                //try relative to base dir
                file = new File(baseDirectory, file.getPath());
                if (file.exists()) {
                    return file;
                }
            }
        }

        //look for a generic resource if no parent specified
        if ( parent == null ) {
            final Resource resource = getResource(location);
    
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
    public final File find(final String... location) throws IOException {
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
    public final File find(final File parent, final String... location) throws IOException {
        return find( parent, concat( location ) );
    }

    /**
     * Helper method to build up a file path from components.
     */
    private String concat(final String... location) {
        StringBuilder loc = new StringBuilder();
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
    public final File findOrCreateDirectory(final String... location) throws IOException {
        return findOrCreateDirectory(null,location);
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parent The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     */
    public final File findOrCreateDirectory(final File parent, final String... location ) throws IOException {
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
    public final File findOrCreateDirectory(final String location) throws IOException {
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
    public final File findOrCreateDirectory(final File parent, final String location) throws IOException {
        final File dir = find( parent, location );
        if ( dir != null ) {
            if ( !dir.isDirectory() ) {
                //location exists, but is a file
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("LOCATION_SPECIFIES_A_FILE"), location);
                throw new IllegalArgumentException(msg);
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
    public final File createDirectory(final String... location) throws IOException {
        return createDirectory(null,location);
    }
    
    /**
     * Creates a new directory specifying components of the location, and the containing directory.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     */
    public final File createDirectory(final File parent, final String... location) throws IOException {
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
    public final File createDirectory(final String location) throws IOException {
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
    public final File createDirectory(final File parent, final String location) throws IOException {
        File file = find(parent,location);

        if (file != null) {
            if (!file.isDirectory()) {
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("LOCATIONS_ALREADY_EXISTS_AND_IS_NOT_DIRECTORY"), location );
                throw new IOException(msg);
            }
        }

        file = parent != null ? new File(parent,location) : new File(location);

        if (file.isAbsolute()) {
            try {
                final boolean dir = file.mkdirs();
                if (!dir) {
                    final String msg = java.text.MessageFormat.format(
                            RESOURCE_BUNDLE.getString("THERE_ARE_SOME_PROBLEMS"), file.getAbsolutePath());
                    throw new IOException(msg);
                }
            } catch (SecurityException se) {
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("PROBABLY_YOU_DO_NOT_HAVE_APPROPRIATE_RIGHTS"), se.getMessage());
                LOGGER.log(Level.SEVERE, msg, se);
                throw se;
            }
            return file;
        }

        //no base directory set, cannot create a relative path
        if (baseDirectory == null) {
             final String msg = java.text.MessageFormat.format(
                     RESOURCE_BUNDLE.getString("NO_BASE_LOCATION_SET_COULD_NOT_CREATE_DIRECTORY"), location);
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

        //cut and past :-(
        try {
            final boolean dir = file.mkdirs();
            if (!dir) {
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("THERE_ARE_SOME_PROBLEMS_WHEN_CREATING_A_DIRECTORY"), file.getAbsolutePath());
                throw new IOException(msg);
            }
        } catch (SecurityException se) {
            final String msg = java.text.MessageFormat.format(
                    RESOURCE_BUNDLE.getString("PROBABLY_YOU_DO_NOT_HAVE_APPROPRIATE_RIGHTS"), se.getMessage());
            LOGGER.log(Level.SEVERE, msg, se);
            throw se;
        }
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
    public final File createFile(final String ...location) throws IOException {
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
    public final File createFile(final String location) throws IOException {
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
    public final File createFile(final File parent, final String... location) throws IOException{
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
    public final File createFile(final File parent, final String location) throws IOException{
        File file = find(parent,location);

        if (file != null) {
            if (file.isDirectory()) {
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("LOCATIONS_ALREADY_EXISTS_AND_IS_DIRECTORY"), location );
                throw new IOException(msg);
            }

            return file;
        }

        file = parent != null ? new File(parent,location) : new File(location);

        if (file.isAbsolute()) {
            try {
                final boolean newFile = file.createNewFile();
                if (!newFile) {
                    final String msg = java.text.MessageFormat.format(
                            RESOURCE_BUNDLE.getString("THERE_ARE_SOME_PROBLEMS_WHEN_CREATING_A_FILE1"), file.getAbsolutePath());
                    LOGGER.log(Level.SEVERE, msg);
                    throw new IOException(msg);
                }
            } catch (IOException ioe) {
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("THERE_ARE_SOME_PROBLEMS_WHEN_CREATING_A_FILE2"), file.getAbsolutePath(), ioe.getMessage());
                LOGGER.log(Level.SEVERE, msg, ioe);
                throw new IOException(msg);
            } catch (SecurityException se) {                
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("PROBABLY_YOU_DO_NOT_HAVE_APPROPRIATE_RIGHTS"), se.getMessage());
                LOGGER.log(Level.SEVERE, msg, se);
                throw se;
            }

            return file;
        }

        if ( parent == null ) {
            //no base directory set, cannot create a relative path
            if (baseDirectory == null) {
                String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("NO_BASE_LOCATION_SET"), location);
                throw new IOException(msg);
            }

            file = new File(baseDirectory, location);
            if (!file.getParentFile().exists()) {
                try {
                    final boolean dir = file.getParentFile().mkdirs();
                    if (!dir) {
                        final String msg = java.text.MessageFormat.format(RESOURCE_BUNDLE
                                .getString("THERE_ARE_SOME_PROBLEMS_WHEN_CREATING_A_DIRECTORY"), file.getParentFile().getAbsolutePath());
                        LOGGER.log(Level.SEVERE, msg);
                        throw new IOException(msg);
                    }
                } catch (SecurityException se) {
                    final String msg = java.text.MessageFormat.format(RESOURCE_BUNDLE
                            .getString("PROBABLY_YOU_DO_NOT_HAVE_APPROPRIATE_RIGHTS"), file.getParentFile().getAbsolutePath());
                    LOGGER.log(Level.SEVERE, msg, se);
                    throw se;
                }
            }
            //cut and past
            try {
                final boolean newFile = file.createNewFile();
                if (!newFile) {
                    final String msg = java.text.MessageFormat.format(
                            RESOURCE_BUNDLE.getString("THERE_ARE_SOME_PROBLEMS_WHEN_CREATING_A_FILE1"), file.getAbsolutePath());
                    LOGGER.log(Level.SEVERE, msg);
                    throw new IOException(msg);
                }
            } catch (IOException ioe) {
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("THERE_ARE_SOME_PROBLEMS_WHEN_CREATING_A_FILE2"), file.getAbsolutePath(), ioe.getMessage());
                LOGGER.log(Level.SEVERE, msg, ioe);
                throw new IOException(msg);
            } catch (SecurityException se) {                
                final String msg = java.text.MessageFormat.format(
                        RESOURCE_BUNDLE.getString("PROBABLY_YOU_DO_NOT_HAVE_APPROPRIATE_RIGHTS"), se.getMessage());
                LOGGER.log(Level.SEVERE, msg, se);
                throw se;
            }
            
        }
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
    public final void copyFromClassPath(final String resource, final String to) throws IOException {
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
    public final void copyFromClassPath(final String resource, final File target ) throws IOException {
        copyFromClassPath( resource, target, null );
    }
    
    /**
     * Copies a resource relative to a particular class from the classpath to the specified file. 
     */
    public final void copyFromClassPath(final String resource, final File target, final Class scope ) throws IOException {
        InputStream is = null; 
        OutputStream os = null;
        final byte[] buffer = new byte[4096];
        int read;

        try{
            // Get the resource
            if (scope == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);    
                if(is==null) {
                    final String msg = java.text.MessageFormat.format(
                            RESOURCE_BUNDLE.getString("COULD_NOT_LOAD_FROM_SCOPE"), resource, Thread.currentThread().getContextClassLoader().toString());
                    LOGGER.log(Level.SEVERE, msg);
                    throw new IOException(msg);
                }
            } else {
                is = scope.getResourceAsStream(resource);
                if(is==null) {
                    final String msg = java.text.MessageFormat.format(
                            RESOURCE_BUNDLE.getString("COULD_NOT_LOAD_FROM_SCOPE"), resource, scope.toString());
                    LOGGER.log(Level.SEVERE, msg);
                    throw new IOException(msg);
                }
            }
    
            // Write it to the target
            try {
                os = new FileOutputStream(target);
                while((read = is.read(buffer)) > 0)
                    os.write(buffer, 0, read);
            } catch (FileNotFoundException targetException) {
                final String msg = MessageFormat.format(
                        RESOURCE_BUNDLE.getString("CAN'T_WRITE_TO_FILE"), target.getAbsolutePath(), System.getProperty("user.name"), targetException.getMessage());
                LOGGER.log(Level.SEVERE, msg, targetException);
                throw new IOException(msg);
            } catch (IOException e) {
                final String msg = MessageFormat.format(
                        RESOURCE_BUNDLE.getString("ERROR_TRYING_TO_COPY"), e.getMessage());
                LOGGER.log(Level.WARNING, msg, e);
                throw new IOException(msg);
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
                final String msg = MessageFormat.format(
                        RESOURCE_BUNDLE.getString("ERROR_TRYING_TO_CLOSING"), e.getMessage());
                LOGGER.log(Level.WARNING, msg, e);
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
    public final static String lookupGeoServerDataDirectory(final ServletContext servContext) {
        
        final String[] typeStrs = { "Java environment variable ",
                "Servlet context parameter ", "System environment variable " };

        final String[] varStrs = { "GEOSERVER_DATA_DIR", "GEOSERVER_DATA_ROOT" };

        String dataDirStr = null;
        //String msgPrefix = null;
        //int iVar = 0;
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
                    final String msg = MessageFormat.format(
                            RESOURCE_BUNDLE.getString("FOUND_TO_BE_UNSET"), typeStr, varStr);
                    LOGGER.log(Level.FINER, msg);
                    continue;
                }
                
                // Verify section
                final File fh = new File(value);
                // Being a bit pessimistic here
                final Object[] obj = new Object[]{typeStr, varStr, value};
                if (!fh.exists()) {
                    final String msg = MessageFormat.format(
                            RESOURCE_BUNDLE.getString("FOUND_SET_TO1"), obj);
                    LOGGER.log(Level.FINE, msg);
                    continue;
                }
                if (!fh.isDirectory()) {
                    final String msg = MessageFormat.format(
                             RESOURCE_BUNDLE.getString("FOUND_SET_TO2"), obj);
                    LOGGER.log(Level.FINE, msg);
                    continue;
                }
                if (!fh.canWrite()) {
                    final String msg = MessageFormat.format(
                             RESOURCE_BUNDLE.getString("FOUND_SET_TO3"), obj);
                    LOGGER.log(Level.FINE, msg);
                    continue;
                }

                // Sweet, we can work with this
                dataDirStr = value;
                //iVar = i;
            }
        }
        
        // fall back to embedded data dir
        if(dataDirStr == null){
            dataDirStr = servContext.getRealPath("/data");}
        
        return dataDirStr;
    }
}
