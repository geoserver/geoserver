package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ResourceStore backed by the file system.
 */
public class FileSystemResourceStore implements ResourceStore {
    
    protected File baseDirectory;

    protected FileSystemResourceStore(){
    }
    
    public FileSystemResourceStore(File resourceDirectory) {
        if (resourceDirectory == null) {
            throw new NullPointerException("root resource directory required");
        }
        if( resourceDirectory.isFile()){
            throw new IllegalArgumentException("Directory required, file present at this location " + resourceDirectory );
        }
        if( !resourceDirectory.exists()){
            boolean create = resourceDirectory.mkdirs();
            if( !create ){
                throw new IllegalArgumentException("Unable to create directory " + resourceDirectory );                
            }            
        }
        if( resourceDirectory.exists() && resourceDirectory.isDirectory() ){
            this.baseDirectory = resourceDirectory;
        }
        else {
            throw new IllegalArgumentException("Unable to acess directory " + resourceDirectory );            
        }
    }
    
    private static File file( File file, String path ){
        for( String item : Paths.names(path) ){
            file = new File( file, item );           
        }
        
        return file;
    }
    
    @Override
    public Resource get(String path) {
        return new FileSystemResource( path );
    }
    
    @Override
    public boolean remove(String path) {
        File file = FileSystemResourceStore.file( baseDirectory, path );
        
        return Files.delete( file );
    }
    
    @Override
    public boolean move(String path, String target) {
        File file = FileSystemResourceStore.file( baseDirectory, path );
        File dest = FileSystemResourceStore.file( baseDirectory, target );
        
        if( !file.exists() && !dest.exists()){
            return true; // moving an undefined resource
        }
        
        try {
            return Files.move(file, dest);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to move "+path+" to "+target, e);
        }
    }
    @Override
    public String toString() {
        return "ResourceStore "+baseDirectory;
    }
    
    /**
     * Direct implementation of Resource.
     * <p>
     * This implementation is a stateless data object, acting as a simple handle around a File.
     */
    class FileSystemResource implements Resource {
        String path;
        File file;
        
        public FileSystemResource(String path) {
            this.path = path;
            this.file = FileSystemResourceStore.file( baseDirectory, path );
        }

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
            File actualFile = file();
            if( !actualFile.exists() ){
                throw new IllegalStateException("File not found "+actualFile);
            }
            try {
                return new FileInputStream( file );
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("File not found "+actualFile,e);
            }
        }

        @Override
        public OutputStream out() {
            File actualFile = file();
            if( !actualFile.exists() ){
                throw new IllegalStateException("Cannot access "+actualFile);
            }
            try {
                return Files.out( actualFile );
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Cannot access "+actualFile,e);
            }
        }

        @Override
        public File file() {
            if( !file.exists() ){
                try {
                    File parent = file.getParentFile();
                    if( !parent.exists() ){
                        boolean created = parent.mkdirs();
                        if( !created ){
                            throw new IllegalStateException("Unable to create "+parent.getAbsolutePath() );
                        }
                    }
                    if (parent.isDirectory()){
                        boolean created = file.createNewFile();
                        if( !created ){
                            throw new FileNotFoundException("Unable to create "+file.getAbsolutePath() );
                        }
                    }
                    else {
                        throw new FileNotFoundException("Unable to create"+file.getName()+" - not a directory " + parent.getAbsolutePath() );
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot create "+path, e);
                }
            }
            if( file.isDirectory()){
                throw new IllegalStateException("Directory (not a file) at "+path);
            }
            else {
                return file;
            }
        }

        @Override
        public File dir() {
            if( !file.exists() ){
                try {
                    File parent = file.getParentFile();
                    if( !parent.exists() ){
                        boolean created = parent.mkdirs();
                        if( !created ){
                            throw new IllegalStateException("Unable to create "+parent.getAbsolutePath() );
                        }
                    }
                    if (parent.isDirectory()){
                        boolean created = file.mkdir();
                        if( !created ){
                            throw new FileNotFoundException("Unable to create "+file.getAbsolutePath() );
                        }
                    }
                    else {
                        throw new FileNotFoundException("Unable to create"+file.getName()+" - not a directory " + parent.getAbsolutePath() );
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot create "+path, e);
                }
            }
            if( file.isFile()){
                throw new IllegalStateException("File (not a directory) at "+path);
            }
            else {
                return file;
            }
        }        
        
        @Override
        public long lastmodified() {
            return file.lastModified();
        }

        @Override
        public Resource parent() {
            int split = path.lastIndexOf('/');
            if (split == -1 ){
                return FileSystemResourceStore.this.get(""); // root
            }
            else {
                return FileSystemResourceStore.this.get( path.substring(0,split) );
            }
        }

        @Override
        public Resource get(String resourcePath) {
            if( resourcePath == null ){
                throw new NullPointerException("Resource path required");
            }
            if( "".equals(resourcePath)){
                return this;
            }
            return FileSystemResourceStore.this.get( Paths.path( path, resourcePath ) );
        }
        
        @Override
        public List<Resource> list() {
            String array[] = file.list();
            if( array == null ){
                return null; // not a directory
            }            
            List<Resource> list = new ArrayList<Resource>( array.length );
            for( String filename : array ){
                Resource resource = FileSystemResourceStore.this.get( Paths.path( path, filename ));
                list.add( resource );
            }
            return list;
        }
        @Override
        public Type getType() {
            if( !file.exists() ){
                return Type.UNDEFINED;
            }
            else if (file.isDirectory()){
                return Type.DIRECTORY;
            }
            else if (file.isFile()){
                return Type.RESOURCE;
            }
            else {
                throw new IllegalStateException("Path does not represent a configuration resource: "+path );                
            }
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
            FileSystemResource other = (FileSystemResource) obj;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return file.getAbsolutePath();
        }
        
    }
}
