/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.PageInfo;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

import freemarker.core.ParseException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.MapModel;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Data format for serializing objects as HTML.
 * <p>
 * <h2>Template Lookup</h2>
 * When serializing an object the following this class looks up a template using the 
 * following methods:
 * <ol>
 *   <li>The result of {@link #getTemplateName(Object)} if not null.
 *   <li>The resource uri being requested. If the requested uri is "/rest/foo" then a template 
 *   named "foo.ftl" is searched for.
 *   <li>The class of the object being serialized. If an object of org.acme.Foo is 
 *   being serialized then a template named "Foo.ftl" is searched for.
 * </ol>
 * </p>
 * <p>
 * <h2>Template Variables</h2>
 * Variables provided to the template are created via reflection on the object
 * being serialized and placed into a map called "properties". For instance consider 
 * the following class of object:
 * <pre>
 * class MyObject {
 * 
 *   String getFoo();
 *   
 *   Integer getBar();
 * }
 * </pre>
 * In the template values for the properties "foo" and "bar" would be available via:
 * <pre>
 * ${properties.foo}
 * ${properties.bar}
 * </pre>
 * </p>
 * <p>
 * A variable called "page" is also provided which contains information about the resource being 
 * accessed. See {@link #createPageDetails(Request)} for more details.
 * </p>
 * <p>
 * <h2>Reflection</h2>
 * The template data model is created reflectively via the {@link ObjectToMapWrapper} class. This 
 * class can be extended to provide custom properties. See the javadoc for more details. 
 * </p>
 * 
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class ReflectiveHTMLFormat extends DataFormat {
    
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.rest");

    /**
     * The request/response being serviced
     */
    Request request;
    Response response;
    
    /**
     * The resource
     */
    Resource resource;
    
    /**
     * The class used for reflection
     */
    protected Class clazz;

    /**
     * Creates a new instance of the format.
     */
    public ReflectiveHTMLFormat(Request request,Response response,Resource resource) {
        this(null,request,response,resource);
    }

    /**
     * Creates a new instance of the format specifying the type of object being serialized.
     * <p>
     * This constructor is useful when reflection should be executed against an interface as opposed
     * to the concrete class of object being serialized. 
     * </p>
     */
    public ReflectiveHTMLFormat( Class clazz, Request request,Response response, Resource resource ) {
        super( MediaType.TEXT_HTML );
        this.clazz = clazz;
        this.request = request;
        this.resource = resource;
    }
    
    @Override
    public Representation toRepresentation(Object object) {
   
        Class clazz = this.clazz != null ? this.clazz : object.getClass();
        Configuration configuration = createConfiguration(object, clazz);
        final ObjectWrapper wrapper = configuration.getObjectWrapper();
        configuration.setObjectWrapper(new ObjectWrapper() {
            public TemplateModel wrap(Object obj) throws TemplateModelException {
                TemplateModel model = wrapper.wrap(obj);
                if ( model instanceof SimpleHash ) {
                    SimpleHash hash = (SimpleHash) model;
                    if ( hash.get( "page" ) == null ) {
                        PageInfo pageInfo = (PageInfo) request.getAttributes().get( PageInfo.KEY );
                        if ( pageInfo != null ) {
                            hash.put( "page", pageInfo );    
                        }
                    }
                }
                return model;
            }
        });
        
        Template template = null;
        
        //first try finding a name directly
        String templateName = getTemplateName( object );
        if ( templateName != null ) {
            template = tryLoadTemplate(configuration, templateName);
            if(template == null)
                template = tryLoadTemplate(configuration, templateName + ".ftl");
        }
        
        //next look up by the resource being requested
        if ( template == null && request != null ) {
            //could not find a template bound to the class directly, search by the resource
            // being requested
            String r = request.getResourceRef().getLastSegment();
            if(r.equals(""))
                r = request.getResourceRef().getBaseRef().getLastSegment();
            int i = r.lastIndexOf( "." ); 
            if ( i != -1 ) {
                r = r.substring( 0, i );
            }
            
            template = tryLoadTemplate(configuration, r + ".ftl");
        }
        
        //finally try to find by class
        while( template == null && clazz != null ) {
            template = tryLoadTemplate(configuration, clazz.getSimpleName() + ".ftl");
            if(template == null) {
                for ( Class interfaze : clazz.getInterfaces() ) {
                    template = tryLoadTemplate(configuration, interfaze.getSimpleName() + ".ftl" );
                    if(template != null)
                        break;
                }
            }
            
            //move up the class hierachy to continue to look for a matching template
            if ( clazz.getSuperclass() == Object.class ) {
                break;
            }
            clazz = clazz.getSuperclass();
        }
        
        if ( template != null ) {
            templateName = template.getName();
        }
        else {
            //use a fallback
            templateName = "Object.ftl";
        }
        
        return new TemplateRepresentation( templateName, configuration, object, getMediaType() );
    }
    
    /**
     * Tries to load a template, will return null if it's not found. If the template exists
     * but it contains syntax errors an exception will be thrown instead
     * 
     * @param configuration The template configuration.
     * @param templateName The name of the template to load.
     */
    protected Template tryLoadTemplate(Configuration configuration, String templateName) {
        try {
            return configuration.getTemplate(templateName);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        } catch(IOException io) {
            LOGGER.log(Level.FINE, "Failed to lookup template " + templateName, io);
            return null;
        }
    }

    protected Configuration createConfiguration(Object data, Class clazz) {
        Configuration cfg = new Configuration( );
        cfg.setObjectWrapper( new ObjectToMapWrapper( clazz ));
        cfg.setClassForTemplateLoading(ReflectiveHTMLFormat.class,"");
        
        return cfg;
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Object readRepresentation(Representation representation) {
        throw new UnsupportedOperationException( "Reading not implemented.");
    }

    /**
     * A hook into the template look-up mechanism.
     * <p>
     * This implementation returns null but subclasses may overide to explicitly specify the name
     * of the template to be used.
     * </p>
     * @param data The object being serialized.
     */
    protected String getTemplateName( Object data ) {
        return null;
    }

    /**
     * Wraps the object being serialized in a {@link SimpleHash} template model.
     * <p>
     * The method {@link #wrapInternal(Map, SimpleHash, Object)} may be overriden to customize 
     * the returned model.
     * </p>
     */
    protected class ObjectToMapWrapper<T> extends BeansWrapper {

        /**
         * The class of object being serialized.
         */
        Class<T> clazz;

        public ObjectToMapWrapper( Class<T> clazz ) {
            this.clazz = clazz;
        }

        @Override
        public TemplateModel wrap(Object object) throws TemplateModelException {
            if ( object instanceof Collection ) {
                Collection c = (Collection) object;
                if (c.isEmpty()) {
                    SimpleHash hash = new SimpleHash();
                    hash.put( "values", new CollectionModel( c, this ) );
                    return hash;
                }
                else {
                    Object o = c.iterator().next();
                    if ( clazz.isAssignableFrom( o.getClass() ) ) {
                        SimpleHash hash = new SimpleHash();
                        hash.put( "values", new CollectionModel( c, this ) );
                        return hash;
                    }    
                }
            }
            
            if ( object != null && clazz.isAssignableFrom( object.getClass() ) ) {
                HashMap map = new HashMap();
                
                ClassProperties cp = OwsUtils.getClassProperties(clazz); 
                for ( String p : cp.properties() ) {
                    if ( "Class".equals( p ) ) continue;
                    Object value = OwsUtils.get(object, p);
                    if ( value == null ) {
                        value = "null";
                    }
                    
                    map.put( Character.toLowerCase(p.charAt(0)) + p.substring(1), value.toString());    
                    
                }
           
                SimpleHash model = new SimpleHash();
                model.put( "properties", new MapModel(map, this) );
                model.put( "className", clazz.getSimpleName() );
                
                wrapInternal(map, model, (T) object);
                return model;
            }
            
            return super.wrap(object);
        }

        /**
         * Template method to customize the returned template model.
         * 
         * @param properties A map of properties obtained reflectively from the object being 
         * serialized.
         * @param model The resulting template model.
         * @param object The object being serialized.
         */
        protected void wrapInternal(Map properties, SimpleHash model, T object ) {
        }

    }

}
