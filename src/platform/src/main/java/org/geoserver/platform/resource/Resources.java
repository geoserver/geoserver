package org.geoserver.platform.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.platform.resource.Resource.Type;

/**
 * Utility methods for working with {@link ResourceStore}.
 * 
 * These methods are suitable for static import and are intended automate common tasks.
 * 
 * @author Jody Garnett
 */
public class Resources {
    
    /**
     * Test if the file or directory denoted by this resource exists.
     * 
     * @see File#exists()
     * @param resource
     * @return true if file or directory exists
     */
    public static boolean exists( Resource resource ){
        return resource != null && resource.getType() != Resource.Type.UNDEFINED;
    }
    
    /**
     * Checks {@link Resource#getType()} and returns existing file() or dir()
     * as appropriate, or null for {@link Resource.Type#UNDEFINED}.
     * 
     * This approach is a reproduction of GeoServerResourceLoader find logic.
     * 
     * @see Resource#dir()
     * @see Resource#file()
     * 
     * @param resource
     * @return Existing file, or null for {@link Resource.Type#UNDEFINED}.
     */
    public static File find( Resource resource ){
        if( resource == null ){
            return null;
        }
        switch ( resource.getType()) {
        case DIRECTORY:
            return resource.dir();
            
        case RESOURCE:
            return resource.file();
            
        default:
            return null;
        }
    }
    
    /**
     * Checks {@link Resource#getType()} and returns existing dir() if available, or null for {@link Resource.Type#UNDEFINED}
     * or {@link Resource.Type#FILE}.
     * 
     * This approach is a reproduction of GeoServerDataDirectory findDataDir logic and will not create a new directory.
     * 
     * @see Resource#dir()
     * 
     * @param resource
     * @return Existing directory, or null
     */
    public static File directory( Resource resource ){
        if( resource != null && resource.getType() == Type.DIRECTORY ){
            return resource.dir();
        }
        else {
            return null;
        }
    }

    /**
     * Checks {@link Resource#getType()} and returns existing file() if available, or null for {@link Resource.Type#UNDEFINED}
     * or {@link Resource.Type#DIRECTORY}.
     * 
     * This approach is a reproduction of GeoServerDataDirectory findDataFile logic and will not create a new file.
     * 
     * @see Resource#file()
     * 
     * @param resource
     * @return Existing file, or null
     */
    public static File file( Resource resource ){
        if( resource != null && resource.getType() == Type.RESOURCE ){
            return resource.file();
        }
        else {
            return null;
        }
    }
    
    /**
     * Create a new directory for the provided resource (this will only work for {@link Resource.Type#UNDEFINED}).
     * 
     * This approach is a reproduction of GeoServerResourceLoader createNewDirectory logic.
     * 
     * @param resource
     * @return newly created file
     * @throws IOException
     */
    public static File createNewDirectory( Resource resource ) throws IOException {
        switch( resource.getType() ){
        case DIRECTORY:
            throw new IOException("New directory "+ resource.path() + " already exists as DIRECTORY");
        case RESOURCE:
            throw new IOException("New directory "+ resource.path() + " already exists as RESOURCE");
        case UNDEFINED:
            return resource.dir(); // will create directory as needed
        default:
            return null; 
        }
    }
    
    /**
     * Create a new file for the provided resource (this will only work for {@link Resource.Type#UNDEFINED}).
     * 
     * This approach is a reproduction of GeoServerResourceLoader createNewFile logic.
     * 
     * @param resource
     * @return newly created file
     * @throws IOException
     */
    public static File createNewFile( Resource resource ) throws IOException {
        switch( resource.getType() ){
        case DIRECTORY:
            throw new IOException("New file "+ resource.path() + " already exists as DIRECTORY");
        case RESOURCE:
            throw new IOException("New file "+ resource.path() + " already exists as RESOURCE");
        case UNDEFINED:
            return resource.file(); // will create directory as needed
        default:
            return null; 
        }
    }
    
    /**
     * Empty placeholder for ResourceStore.
     * <p>
     * Empty placeholder intended for test cases (used as spring context default when a base directory is not provided).
     * This implementation prevents client code from requiring null checks on {@link ResourceStore#get(String)}. IllegalStateException
     * are thrown by in(), out() and file() which are the usual methods clients require error handling.  
     */
    public static ResourceStore EMPTY = new ResourceStore() {
        final long MODIFIED = System.currentTimeMillis();
        @Override
        public Resource get(final String resourcePath) {
            return new Resource(){
                String path = resourcePath;
                @Override
                public String path() {
                    return path;
                }

                @Override
                public String name() {
                    return Paths.name( path );
                }

                @Override
                public InputStream in() {
                    throw new IllegalStateException("Unable to read from ResourceStore.EMPTY");
                }

                @Override
                public OutputStream out() {
                    throw new IllegalStateException("Unable to write to ResourceStore.EMPTY");
                }

                @Override
                public File file() {
                    throw new IllegalStateException("No file access to ResourceStore.EMPTY");
                }

                @Override
                public File dir() {
                    throw new IllegalStateException("No directory access to ResourceStore.EMPTY");
                }
                
                @Override
                public long lastmodified() {
                    return MODIFIED;
                }

                @Override
                public Resource parent() {
                    return EMPTY.get(Paths.parent(path));
                }

                public Resource get(String resourcePath) {
                    return EMPTY.get(Paths.path(this.path, resourcePath));
                }
                
                @Override
                public List<Resource> list() {
                    return null;
                }

                @Override
                public Type getType() {
                    return Type.UNDEFINED;
                }

                @Override
                public int hashCode() {
                    final int prime = 31;
                    int result = 1;
                    result = prime * result + ((path == null) ? 0 : path.hashCode());
                    return result;
                }
                
                @Override
                public boolean equals(Object obj) {
                    if (this == obj)
                        return true;
                    if (obj == null)
                        return false;
                    if (getClass() != obj.getClass())
                        return false;
                    Resource other = (Resource) obj;
                    if (path == null) {
                        if (other.path() != null)
                            return false;
                    } else if (!path.equals(other.path()))
                        return false;
                    return true;
                }

                @Override
                public String toString() {
                    return path;
                }
            };
        }
        public String toString() {
            return "Resources.EMPTY";
        }
        @Override
        public boolean remove(String path) {
            return false; // unable to remove empty resource
        }
        @Override
        public boolean move(String path, String target) {
            return false; // unable to move empty resource
        }
    };
    /**
     * Search for resources using pattern and last modified time.
     * 
     * @param resource
     * @param lastModified
     * @return list of modified resoruces
     */
    public static List<Resource> search(Resource resource, long lastModified) {
        if (resource.getType() == Type.DIRECTORY) {
            ArrayList<Resource> results = new ArrayList<Resource>();
            for( Resource child : resource.list() ){
                switch ( child.getType()) {
                case RESOURCE:
                    if( child.lastmodified() > lastModified ){
                        results.add( child );
                    }                    
                    break;

                default:
                    break;
                }
            }
            return results;
        }
        return Collections.emptyList();
    }
}