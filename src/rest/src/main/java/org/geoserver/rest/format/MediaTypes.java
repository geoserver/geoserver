/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MultiHashMap;
import org.restlet.data.MediaType;

/**
 * Singleton for managing mappings to {@link MediaType} objects.
 * 
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class MediaTypes {

    /**
     * mapping from extension to media type.
     */
    HashMap<String,MediaType> extensions = new HashMap<String,MediaType>();
    
    /**
     * mapping of media type to synonyms for that media type.
     */
    MultiHashMap synonyms = new MultiHashMap();
    
    private MediaTypes() {
    }
    static MediaTypes instance = new MediaTypes();
    
    /**
     * Registers a mapping from extension to media type.
     * <p>
     * An example of calling this method would be:
     * <pre>
     * MediaTypes.registerExtension( "xml", MediaType.APPLICATION_XML );
     * </pre>
     * </p>
     * @param ext The extension.
     * @param mediaType The media type.
     */
    public static void registerExtension( String ext, MediaType mediaType ) {
        instance.extensions.put( ext, mediaType );
    }
    
    /**
     * Returns the media type mapped to an extension, or null if no such mapping exists.
     * 
     * @param ext The extension.
     * 
     * @return The media type, or null.
     * @see {@link #registerExtension(String, MediaType)}
     */
    public static MediaType getMediaTypeForExtension( String ext ) {
        return instance.extensions.get( ext );
    }
    
    /**
     * Returns the media type mapped to an extension, or null if no such mapping exists.
     * 
     * @param mediaType the media type.
     * 
     * @return THe extension, or null.
     * @see {@link #registerExtension(String, MediaType)}
     */
    public static String getExtensionForMediaType( MediaType mediaType ) {
        for ( Map.Entry<String, MediaType> e : instance.extensions.entrySet() ) {
            if ( e.getValue().equals( mediaType ) ) {
                return e.getKey();
            }
        }
        return null;
    }
    
    /**
     * Registers a synonym between to media types.
     * <p>
     * A synonym is considered to be an equivlance for functional purposes. An example would be 
     * registering a synonym for {@link MediaType#APPLICATION_XML} as {@link MediaType#TEXT_XML}.
     * </p>
     * @param mediaType The type.
     * @param synonym A synonym for the type.
     */
    public static void registerSynonym( MediaType mediaType, MediaType synonym ) {
        instance.synonyms.put( mediaType, synonym );
    }
    
    /**
     * Returns the list of media types which are synonymous to the specfied type.
     * 
     * @param mediaType The type.
     * 
     * @return A list of media types, or an empty list.
     * @see {@link #registerSynonym(MediaType, MediaType)}.
     */
    public static List<MediaType> getSynonyms( MediaType mediaType ) {
        List syns = (List) instance.synonyms.getCollection( mediaType ); 
        return syns != null ? Collections.unmodifiableList( syns ) : Collections.EMPTY_LIST; 
    }
    
}
