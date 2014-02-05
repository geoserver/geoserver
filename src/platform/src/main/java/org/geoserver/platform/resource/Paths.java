package org.geoserver.platform.resource;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for handling Resource paths in a consistent fashion.
 * <p>
 * This utility class is primarily aimed at implementations of ResourceStore and may be helpful
 * when writing test cases. These methods are suitable for static import.
 * <p>
 * Resource paths are consistent with file URLs. The base location is represented with "", relative paths are not supported.
 * 
 * @author Jody Garnett
 */
public class Paths {
    /**
     * Path to base resource.
     */
    public static final String BASE = "";

    static String parent( String path ){
        if( path == null ){
            return null;
        }
        int last = path.lastIndexOf('/');
        if( last == -1 ){
            if( BASE.equals(path) ){
                return null;
            }
            else {
                return BASE;
            }
        }
        else {
            return path.substring(0,last);
        }
    }
    
    static String name( String path ){
        if( path == null ){
            return null;
        }
        int last = path.lastIndexOf('/');
        if( last == -1 ){            
            return path; // top level resource
        }
        else {
            String item = path.substring(last+1);
            return item;
        }
    }

    static String extension( String path ){
        String name = name(path);
        if( name == null ){
            return null;
        }
        int last = name.lastIndexOf('.');
        if( last == -1 ){
            return null; // no extension
        }
        else {
            return name.substring(last+1);
        }
    }
    
    static String sidecar( String path, String extension){
        if( extension == null ){
            return null;
        }
        int last = path.lastIndexOf('.');
        if( last == -1 ){
            return path+"."+extension;
        }
        else {
            return path.substring(0,last)+"."+extension;
        }
    }

    /**
     * Path construction.
     * 
     * @param path Items defining a Path
     * @return path Path used to identify a Resource
     */
    public static String path( String... path ){
        if( path == null || (path.length == 1 && path[0] == null)){
            return null;
        }
        ArrayList<String> names = new ArrayList<String>();
        for( String item : path ){
            names.addAll( names( item ) );
        }
        return toPath(names);
    }
    
    private static String toPath(List<String> names) {
        StringBuilder buf = new StringBuilder();
        final int LIMIT = names.size();
        for( int i=0; i<LIMIT; i++) {
            String item = names.get(i);
            if( INVALID.contains(item)){
                throw new IllegalArgumentException( "Contains invalid "+ item + " path: "+buf.toString());
            }
            if( item != null ){
                buf.append(item);
                if(i<LIMIT-1){
                    buf.append("/");
                }
            }
        }
        return buf.toString();
    }
    static final Set<String> INVALID = new HashSet<String>(Arrays.asList(new String[]{"..","."}));
    
    /**
     * Path construction relative to directory.
     * 
     * @param directory Directory Path
     * @param path items
     * @return path
     */
//    public static String relative( String directory, String... path ){
//        ArrayList<String> names = new ArrayList<String>();
//        if( directory != null ){
//            names.addAll( names(directory));
//        }
//        if( path != null ){
//            for( String item : path ){
//                names.addAll( names( item ) );
//            }
//        }        
//        ArrayList<String> resolvedPath = new ArrayList<String>( names.size());
//        for( String item : names ){
//            if( item == null ) continue;
//            if( item.equals(".")) continue;
//            if( item.equals("..")){
//                if( !resolvedPath.isEmpty() ){
//                    resolvedPath.remove( resolvedPath.size()-1);
//                    continue;
//                }
//                else {
//                    throw new IllegalStateException("Path location "+item+" outside of "+directory);
//                }
//            }
//            resolvedPath.add(item);
//        }
//        return toPath( resolvedPath );
//    }
    
    public static List<String> names(String path){
        if( path == null || path.length()==0){
            return Collections.emptyList();
        }
        int index=0;
        int split = path.indexOf('/');
        if( split == -1){
            return Collections.singletonList(path);
        }
        ArrayList<String> names = new ArrayList<String>(3);
        String item;
        do {
            item = path.substring(index,split);
            if( item != "/"){
                names.add( item );
            }
            index = split+1;
            split = path.indexOf('/', index);
        }
        while( split != -1);
        item = path.substring(index);
        if( item != null && item.length()!=0 && item != "/"){
            names.add( item );
        }
        
        return names;
    }
    
    /**
     * Convert to file to resource path.
     * 
     * @param base directory location
     * @param file relative file reference
     * @return relative path used for Resource lookup
     */
    public static String convert(File base, File file) {
        if (base == null) {
            if (file.isAbsolute()) {
                throw new NullPointerException("Unable to determine relative path");
            } else {
                return convert(file.getPath());
            }
        }
        if( file == null ){
            return Paths.BASE;
        }
        URI baseURI = base.toURI();
        URI fileURI = file.toURI();
        
        if( fileURI.toString().startsWith( baseURI.toString())){
            URI relativize = baseURI.relativize(fileURI);
    
            return relativize.getPath();
        }
        else {
            return convert( file.getPath() );
        }
    }

    /**
     * Convert to file to resource path, allows for relative references (but is limited to content within the provided base directory).
     * 
     * 
     * @param base directory location
     * @param folder context for relative path (may be "." or null for base directory)
     * @param fileLocation File path (using {@link File#separator}) allowing for relative references
     * @return relative path used for Resource lookup
     */
    public static String convert(File base, File folder, String fileLocation) {
        if (base == null) {
            throw new NullPointerException("Base directory required for relative path");
        }
        List<String> folderPath = names(convert( base, folder ));
        List<String> filePath = names(convert(fileLocation));
        
        List<String> resolvedPath = new ArrayList<String>( folderPath.size()+filePath.size() );
        resolvedPath.addAll(folderPath);
        
        for( String item : filePath ){
           if( item == null ) continue;
           if( item.equals(".")) continue;
           if( item.equals("..")){
               if( !resolvedPath.isEmpty() ){
                   resolvedPath.remove( resolvedPath.size()-1);
                   continue;
               }
               else {
                   throw new IllegalStateException("File location "+fileLocation+" outside of "+base.getPath());
               }
           }
           resolvedPath.add(item);
        }
        return toPath( resolvedPath );
    }
    
    /**
     * Convert to file to resource path, allows for relative references (but is limited to content within the provided base directory).
     * 
     * 
     * @param base directory location
     * @param folder context for relative path (may be "." or null for base directory)
     * @param fileLocation File path (using {@link File#separator}) allowing for relative references
     * @return relative path used for Resource lookup
     */
    public static String convert(File base, File folder, String ...location) {
        if (base == null) {
            throw new NullPointerException("Base directory required for relative path");
        }
        List<String> folderPath = names(convert( base, folder ));
        List<String> filePath = Arrays.asList( location );
        
        List<String> resolvedPath = new ArrayList<String>( folderPath.size()+filePath.size() );
        resolvedPath.addAll(folderPath);
        
        for( String item : filePath ){
           if( item == null ) continue;
           if( item.equals(".")) continue;
           if( item.equals("..")){
               if( !resolvedPath.isEmpty() ){
                   resolvedPath.remove( resolvedPath.size()-1);
                   continue;
               }
               else {
                   throw new IllegalStateException("File location "+filePath+" outside of "+base.getPath());
               }
           }
           resolvedPath.add(item);
        }
        return toPath( resolvedPath );
    }
    
    /**
     * Convert a filePath to resource path (relative to base directory), this method
     * does not support absolute file paths.
     * 
     * This method converts file paths (using {@link File#separator}) to the URL style paths used for {@link ResourceStore#get(String)}.
     * 
     * @param directory directory used to resolve relative reference lookup
     * @param filePath File path using {@link File#separator}
     * @return Resource path suitable for use with {@link ResourceStore#get(String)} or null for absolute path
     */
    public static String convert( String filePath ){        
        if( filePath == null ) {
            return null;
        }
        if ( filePath.length()==0) {
            return filePath;
        }
        if( File.separatorChar == '/'){
            return filePath;
        }
        else {
            return filePath.replace(File.separatorChar, '/');
        }
    }
    
    /**
     * Convert a filePath to resource path (starting from the provided path). Absolute file paths are not supported, and the final resource must still
     * be within the data directory.
     * 
     * This method converts file paths (using {@link File#separator}) to the URL style paths used for {@link ResourceStore#get(String)}.
     * 
     * @param path Initial path used resolve relative reference lookup
     * @param filePath File path using {@link File#separator}
     * @return Resource path suitable for use with {@link ResourceStore#get(String)} or null for absolute path
     */
    public static String convert( String path, String filename ){        
        if (path == null) {
            throw new NullPointerException("Initial path required to handle relative filenames");
        }
        List<String> folderPath = names(path);
        List<String> filePath = names(convert(filename));
        
        List<String> resolvedPath = new ArrayList<String>( folderPath.size()+filePath.size() );
        resolvedPath.addAll(folderPath);
        
        for( String item : filePath ){
           if( item == null ) continue;
           if( item.equals(".")) continue;
           if( item.equals("..")){
               if( !resolvedPath.isEmpty() ){
                   resolvedPath.remove( resolvedPath.size()-1);
                   continue;
               }
               else {
                   throw new IllegalStateException("File location "+filename+" outside of "+path);
               }
           }
           resolvedPath.add(item);
        }
        return toPath( resolvedPath );
    }
    
}