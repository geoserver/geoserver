/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
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
    public static boolean exists(Resource resource) {
        return resource != null && resource.getType() != Resource.Type.UNDEFINED;
    }

    /**
     * Checks {@link Resource#getType()} and returns existing file() or dir() as appropriate, or null for {@link Resource.Type#UNDEFINED}.
     * 
     * This approach is a reproduction of GeoServerResourceLoader find logic.
     * 
     * @see Resource#dir()
     * @see Resource#file()
     * 
     * @param resource
     * @return Existing file, or null for {@link Resource.Type#UNDEFINED}.
     */
    public static File find(Resource resource) {
        if (resource == null) {
            return null;
        }
        switch (resource.getType()) {
        case DIRECTORY:
            return resource.dir();

        case RESOURCE:
            return resource.file();

        default:
            return null;
        }
    }
    
    /**
     * Checks {@link Resource#getType()} and returns existing dir() if available, or null for {@link Resource.Type#UNDEFINED} or
     * {@link Resource.Type#FILE}.
     * 
     * This approach is a reproduction of GeoServerDataDirectory findDataDir logic and will not create a new directory.
     * 
     * @see Resource#dir()
     * 
     * @param resource
     * @return Existing directory, or null
     */
    public static File directory(Resource resource) {
        return directory(resource, false);
    }
    
    /**
     * If create is true or if a directory exists returns resource.dir, otherwise it returns null.
     * 
     * @see Resource#dir()
     * 
     * @param resource
     * @param create
     * @return directory, or null
     */
    public static File directory(Resource resource, boolean create) {
        final File f;
        if(resource==null) {
            f = null;
        } else if(create) {
            f = resource.dir();
        } else {
            if (resource.getType() == Type.DIRECTORY) {
                f = resource.dir();
            } else {
                f = null;
            }
        }
        return f;
    }
    
    /**
     * Checks {@link Resource#getType()} and returns existing file() if available, or null for {@link Resource.Type#UNDEFINED} or
     * {@link Resource.Type#DIRECTORY}.
     * 
     * This approach is a reproduction of GeoServerDataDirectory findDataFile logic and will not create a new file.
     * 
     * @see Resource#file()
     * 
     * @param resource
     * @return Existing file, or null
     */
    public static File file(Resource resource) {
        return file(resource, false);
    }

    /**
     * If create is true or if a file exists returns resource.file, otherwise it returns null.
     * 
     * @see Resource#file()
     * 
     * @param resource
     * @param create
     * @return file, or null
     */
    public static File file(Resource resource, boolean create) {
        final File f;
        if(resource==null) {
            f = null;
        } else if(create) {
            f = resource.file();
        } else {
            if (resource.getType() == Type.RESOURCE) {
                f = resource.file();
            } else {
                f = null;
            }
        }
        return f;
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
    public static File createNewDirectory(Resource resource) throws IOException {
        switch (resource.getType()) {
        case DIRECTORY:
            throw new IOException("New directory " + resource.path()
                    + " already exists as DIRECTORY");
        case RESOURCE:
            throw new IOException("New directory " + resource.path()
                    + " already exists as RESOURCE");
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
    public static File createNewFile(Resource resource) throws IOException {
        switch (resource.getType()) {
        case DIRECTORY:
            throw new IOException("New file " + resource.path() + " already exists as DIRECTORY");
        case RESOURCE:
            throw new IOException("New file " + resource.path() + " already exists as RESOURCE");
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
            for (Resource child : resource.list()) {
                switch (child.getType()) {
                case RESOURCE:
                    if (child.lastmodified() > lastModified) {
                        results.add(child);
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
    
    /**
     * Write the contents of a stream into a resource
     * @param data data to write
     * @param destination resource to write to
     * @throws IOException
     */
    public static void copy (InputStream data, Resource destination) throws IOException {
        try(OutputStream out = destination.out()) {
            IOUtils.copy(data, out);
        }
    }
    
    /**
     * Write the contents of a resource into another resource
     * @param data resource to read
     * @param destination resource to write to
     * @throws IOException
     */
    public static void copy (Resource data, Resource destination) throws IOException {
        try(InputStream in = data.in()) {
            copy(in, destination);
        }
    }
    
    /**
     * Write the contents of a stream to a new Resource inside a directory
     * @param data data to write
     * @param directory parent directory to create the resource in
     * @param filename file name of the new resource
     * @throws IOException
     */
    public static void copy (InputStream data, Resource directory, String filename) throws IOException {
        copy(data, directory.get(filename));
    }
    
    /**
     * Write the contents of a File to a new Resource with the same name inside a directory
     * @param data data to write
     * @param directory parent directory to create the resource in
     * @throws IOException
     */
    public static void copy (File data, Resource directory) throws IOException {
        String filename = data.getName();
        try(InputStream in = new FileInputStream(data)) {
            copy(data, directory.get(filename));
        }
    }
    
    /**
     * Renames a resource by reading it and writing to the new resource, then deleting the old one.
     * This is not atomic.
     * @param source
     * @param destination
     * @throws IOException
     * @return true if successful, false if either the write or delete failed.
     */
    public static boolean renameByCopy(Resource source, Resource destination) {
        try {
            copy(source, destination);
            return source.delete();
        } catch (IOException e) {
            return false;
        }
    }
}