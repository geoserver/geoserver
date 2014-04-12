/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
     * @return true If resource is not UNDEFINED
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
    
    /** File contents read on demand */
    public static <T> Content<T> watch( final File file, final Content.Read<T> content ){
        return new Content<T>(){
            @Override
            public T content() {
                InputStream in;
                try {
                    in = new FileInputStream(file);
                } catch (FileNotFoundException notFound) {
                    throw new IllegalStateException(notFound);
                }
                try {
                    return content.read(in);
                }
                finally {
                    try {
                        in.close();
                    } catch (IOException notClosed) {
                        throw new IllegalStateException(notClosed);
                    }
                }
            }
        };
    }
    /** Resource contents read on demand */
    public static <T> Content<T> watch( final Resource resource, final Content.Read<T> content ){
        return new Content<T>(){
            @Override
            public T content() {
                InputStream in = resource.in();
                try {
                    return content.read(in);
                }
                finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        };
    }
    public static <T> Content<T> cache( Content<T> watch, long interval ){
        return watch;
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