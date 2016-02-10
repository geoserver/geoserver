/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.Converters;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * Abstract base class for resources.
 * 
 * @author Justin Deoliveira, OpenGEO
 *
 */
public abstract class AbstractResource extends Resource {

    /**
     * media type setup
     */
    static {
        MediaTypes.registerExtension( "html", MediaType.TEXT_HTML );
        MediaTypes.registerExtension( "xml", MediaType.APPLICATION_XML );
        MediaTypes.registerExtension( "json", MediaType.APPLICATION_JSON );
        
        MediaTypes.registerSynonym( MediaType.APPLICATION_XML, MediaType.TEXT_XML );
        MediaTypes.registerSynonym( MediaType.APPLICATION_JSON, new MediaType( "text/json") );
    }
    
    /**
     * list of formats used to read and write representations of the resource.
     */
    protected volatile Map<MediaType,DataFormat> formats;
    
    /**
     * Constructs a new resource from context, request, and response.
     */
    protected AbstractResource( Context context, Request request, Response response ) {
        super( context, request, response );
    }

    /**
     * Constructs a new resource.
     * <p>
     * When using this constructor, {@link #init(Context, Request, Response)} needs to be 
     * called before any other operations.
     * </p>
     */
    protected AbstractResource() {
    }
    
    /**
     * Creates the list of formats used to create and read representations of the resource.
     * <p>
     * The first entry in the list is considered to be the default format. This format will 
     * be used when the client does not explicitly specify another format. 
     * </p>
     */
    protected abstract List<DataFormat> createSupportedFormats(Request request,Response response);
    
    /**
     * Returns the format to use to serialize during a GET request.
     * <p>
     * To determine the format first the "Accepts" header is checked. If not set then the 
     * "extension" of the resource being requested is used, which originates from the route
     * template under the {format} or {type} variable. IF neither the header or extension is
     * set the default format is used, ie the first format returned from 
     * {@link #createSupportedFormats(Request, Response)}.  
     * </p>
     *
     * @param includeFileExtension use file extension such as .xml or .html as hint
     * 
     */
    protected DataFormat getFormatGet(boolean includeFileExtension) {
        DataFormat df = null;
        
        //check if the client specified an extension
        String ext = (String) getRequest().getAttributes().get( "format" );
        if ( ext == null ) {
            ext = (String) getRequest().getAttributes().get( "type" );
        }
        if ( ext == null && includeFileExtension) {
            //try from the resource uri
            String uri = getRequest().getResourceRef() != null ? 
                getRequest().getResourceRef().getLastSegment() : null;
            if ( uri != null ) {
                ext = ResponseUtils.getExtension(uri);
            }
        }
        
        if ( ext != null ) {
            //lookup the media type matching the extension
            MediaType mt = MediaTypes.getMediaTypeForExtension( ext );
            if ( mt != null ) {
                df = lookupFormat(mt);
            }
        }
        
        List<Preference<MediaType>> accepts = null;
        boolean acceptsAll = false;
        if ( df == null ) {
            //next check the Accepts header
            accepts = getRequest().getClientInfo().getAcceptedMediaTypes();
            acceptsAll = accepts.isEmpty();
            for ( Iterator<Preference<MediaType>> i = accepts.iterator(); i.hasNext(); ) {
                Preference<MediaType> pref = i.next();
                if ( pref.getMetadata().equals( MediaType.ALL ) ) {
                    acceptsAll = true;
                    continue;
                }
                
                df = lookupFormat( pref.getMetadata() ); 
                if ( df != null ) {
                    break;
                }
            }
        }
        
        if ( df == null && acceptsAll ) {
            //could not find suitable format, if client specifically did not specify 
            // any accepted formats or accepts all media types, just return the first
            df = getFormats().values().iterator().next();
        }
        
        return df;
    }
    
    /**
     * Returns the format to use to serialize during a GET request.
     * <p>
     * To determine the format first the "Accepts" header is checked. If not set then the 
     * "extension" of the resource being requested is used, which originates from the route
     * template under the {format} or {type} variable. IF neither the header or extension is
     * set the default format is used, ie the first format returned from 
     * {@link #createSupportedFormats(Request, Response)}.  
     * </p>
     *
     */
    protected DataFormat getFormatGet() {
        return getFormatGet(true);
    }

    /**
     * returns the format to use to de-serialize during a POST or PUT request.
     * <p>
     * The format is located by looking up <pre>getRequest().getEntity().getMediaType()</pre> from
     * the list created by {@link #createSupportedFormats()}. 
     * </p>
     */
    protected DataFormat getFormatPostOrPut() {
        MediaType type = getRequest().getEntity().getMediaType();
        if ( type != null ) {
            //DataFormat format = getFormats().get( type.toString() );
            DataFormat format = lookupFormat( type );
            /*
            if ( format == null ) {
                //check the sub type
                String sub = type.getSubType();
                if ( sub != null ) {
                    format = getFormats().get( sub );
                    if ( format == null ) {
                        //check for sub type specified with '+'
                        int plus = sub.indexOf( '+' );
                        if ( plus != -1 ) {
                            sub = sub.substring(0,plus);
                            format = getFormats().get( sub );
                        }
                    }
                }
            }
            */
            if ( format != null ) {
                return format;
            }
        }
        
        throw new RestletException( "Could not determine format. Try setting the Content-type header.",
            Status.CLIENT_ERROR_BAD_REQUEST );
    }
    
    /**
     * Accessor for formats which lazily creates the format map.
     */
    protected Map<MediaType,DataFormat> getFormats() {
        if ( formats == null ) {
            synchronized (this) {
                if ( formats == null ) {
                    formats = new LinkedHashMap();
                    for ( DataFormat format : createSupportedFormats(getRequest(), getResponse()) ) { 
                        formats.put( format.getMediaType(), format );
                    }
                    
                    if ( formats.isEmpty() ) {
                        throw new RuntimeException( "Empty format list" );
                    }
                }
            }
        }
        return formats;
    }
    
    /*
     * Helper method for looking up a format based on a media type.
     */
    DataFormat lookupFormat( MediaType mt ) {
        //exact match
        DataFormat fmt = getFormats().get( mt );
        
        if ( fmt == null ) {
            //check for the case of a media type being "contained"
            for ( MediaType mediaType : getFormats().keySet() ) {
                if ( mediaType.includes( mt ) || mt.includes( mediaType ) ) {
                    fmt = getFormats().get( mediaType );
                    break;
                }
            }
        }
        
        if ( fmt == null ) {
            //do a check for synonyms
            for( MediaType syn : MediaTypes.getSynonyms( mt ) ) {
                fmt = getFormats().get( syn );
                if ( fmt != null ) {
                    break;
                }
            }
        }
        
        if ( fmt == null ) {
            //do a reverse check check for synonyms
            for ( MediaType mediaType : getFormats().keySet() ) {
                for( MediaType syn : MediaTypes.getSynonyms( mediaType ) ) {
                    if ( mt.equals( syn ) ) {
                        fmt = getFormats().get( mediaType );
                    }
                }
            }
        }
        
        if ( fmt == null ) {
            //do a "contains" check on synonyms
            for( MediaType syn : MediaTypes.getSynonyms( mt ) ) {
                for ( MediaType mediaType : getFormats().keySet() ) {
                    if ( mediaType.includes( syn ) || syn.includes( mediaType )) {
                        fmt = getFormats().get( mediaType );
                        break;
                    }
                }
            }
        }
        
        //TODO: reverse contains check on synonyms
        return fmt;
    }
    
    /**
     * Returns the object which contains information about the page / resource bring requested. 
     * <p>
     * This object is created by the rest dispatcher when a request comes in.  
     * </p>
     */
    protected PageInfo getPageInfo() {
        return (PageInfo) getRequest().getAttributes().get( PageInfo.KEY );
    }

    /**
     * Convenience method for subclasses to look up the (URL-decoded)value of
     * an attribute from the request, ie {@link Request#getAttributes()}.
     * 
     * @param attribute THe name of the attribute to lookup.
     * 
     * @return The value as a string, or null if the attribute does not exist
     *     or cannot be url-decoded.
     */
    protected String getAttribute(String attribute) {
        return RESTUtils.getAttribute(getRequest(), attribute);
    }
    
    /**
     * Convenience method for subclasses to look up the (URL-decoded) value of a value specified
     * in the request query string.
     * 
     * @param key The name of the value to lookup. 
     * 
     * @return The converted value, or <code>null</code> if the value was not specified.
     */
    protected String getQueryStringValue(String key) {
        return RESTUtils.getQueryStringValue(getRequest(), key);
    }
    
    /**
     * Convenience method for subclasses to look up the (URL-decoded) value of a value specified
     * in the request query string, converting to the specified type.
     * 
     * @param key The name of the value to lookup. 
     * @param clazz The class to convert to.
     * @param defalt The default value to return if the attribute does not exist or no 
     *   conversion was possible. May be <code>null</code>
     * 
     * @return The converted value, or <code>null</code> if either (a) the value was not 
     *   specified or (b) the value could not be converted to the specified type.
     */
    protected <T> T getQueryStringValue(String key, Class<T> clazz, T defalt) {
        String value = getQueryStringValue(key);
        if (value != null) {
            T converted = Converters.convert(value, clazz);
            if (converted != null) {
                return converted;
            }
        }

        return defalt;
    }

    /**
     *
     * Request headers
     *
     * @return request headers form
     */
    protected Form getRequestHeaders() {
        return RESTUtils.getHeaders(getRequest());
    }

    /**
     *
     * Response headers
     *
     * @return response headers form
     */
    protected Form getResponseHeaders() {
        return RESTUtils.getHeaders(getResponse());
    }
}
