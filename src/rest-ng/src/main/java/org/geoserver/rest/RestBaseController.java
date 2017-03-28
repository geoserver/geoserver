package org.geoserver.rest;

import freemarker.core.ParseException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.MapModel;
import freemarker.template.*;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.rest.wrapper.RestWrapperAdapter;
import org.geotools.util.logging.Logging;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for all rest-ng controllers
 *
 * Extending classes should be annotated with {@link org.springframework.web.bind.annotation.RestController} and
 * {@link org.springframework.web.bind.annotation.ControllerAdvice}
 *
 * Provides basic logic for wrapper construction and persister configuration
 *
 * Also provides various utilities for dealing with the {@link FreemarkerHTMLMessageConverter}
 */
public abstract class RestBaseController implements RequestBodyAdvice {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.rest");

    /**
     * Root path of the rest api
     */
    public static final String ROOT_PATH = "/restng";

    /**
     * Default encoding for the freemarker {@link Configuration}
     */
    protected String encoding = "UTF-8";

    /**
     * Name of the folder containing freemarker templates
     */
    protected String pathPrefix = "templates";

    /**
     * Constructs the freemarker {@link Configuration}
     *
     * @param clazz Class of the object being wrapped
     * @return
     */
    protected <T> Configuration createConfiguration(Class<T> clazz) {
        Configuration cfg = new Configuration( );
        cfg.setObjectWrapper(new ObjectToMapWrapper<>(clazz));
        cfg.setClassForTemplateLoading(getClass(),pathPrefix);
        if (encoding != null) {
            cfg.setDefaultEncoding(encoding);
        }
        return cfg;
    }

    /**
     * Finds a freemarker {@link Template} based on the object and {@link Configuration}
     * @param o Object being serialized
     * @param clazz Class of the object
     * @return Freemarker template
     */
    protected Template getTemplate(Object o, Class clazz) {
        Template template = null;
        Configuration configuration = createConfiguration(clazz);

        //first try finding a name directly
        String templateName = getTemplateName( o );
        if ( templateName != null ) {
            template = tryLoadTemplate(configuration, templateName);
            if(template == null)
                template = tryLoadTemplate(configuration, templateName + ".ftl");
        }
        final RequestInfo requestInfo = RequestInfo.get();

        //next look up by the resource being requested
        if ( template == null && requestInfo != null ) {
            //could not find a template bound to the class directly, search by the resource
            // being requested
            String pagePath = requestInfo.getPagePath();
            String r = pagePath.substring(pagePath.lastIndexOf('/')+1);
            //trim trailing slash
            if(r.equals("")) {
                pagePath = pagePath.substring(0, pagePath.length() - 1);
                r = pagePath.substring(pagePath.lastIndexOf('/')+1);
            }
            int i = r.lastIndexOf( "." );
            if ( i != -1 ) {
                r = r.substring( 0, i );
            }

            template = tryLoadTemplate(configuration, r + ".ftl");
        }

        //finally try to find by class
        while( template == null && clazz != null ) {

            template = tryLoadTemplate(configuration, clazz.getSimpleName() + ".ftl");
            if (template == null) {
                template = tryLoadTemplate(configuration, clazz.getSimpleName().toLowerCase() + ".ftl");
            }
            if(template == null) {
                for (Class<?> interfaze : clazz.getInterfaces()) {
                    template = tryLoadTemplate(configuration, interfaze.getSimpleName() + ".ftl" );
                    if(template != null)
                        break;
                }
            }

            //move up the class hierarchy to continue to look for a matching template
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
        return tryLoadTemplate(configuration, templateName);
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

    /**
     * Template method to get a custom template name
     *
     * @param object The object being serialized.
     */
    protected String getTemplateName(Object object) {
        return null;
    }

    /**
     * Wraps the passed collection in a {@link RestListWrapper}
     *
     * @param list The collection to wrap
     * @param clazz The advertised class to use for the collection contents
     * @return
     */
    protected <T> RestWrapper<T> wrapList(Collection<T> list, Class<T> clazz) {
        return new RestListWrapper<T>(list, clazz, this, getTemplate(list, clazz));
    }

    /**
     * Wraps the passed object in a {@link RestWrapperAdapter}
     *
     * @param object The object to wrap
     * @param clazz The advertised class to use for the collection contents
     * @return
     */
    protected <T> RestWrapper<T> wrapObject(T object, Class<T> clazz) {
        return new RestWrapperAdapter<T>(object, clazz, this, getTemplate(object, clazz));
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (!(inputMessage instanceof RestHttpInputWrapper)) {
            return new RestHttpInputWrapper(inputMessage, this);
        }
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /**
     * Default (empty) implementation of configurePersister. This will be called by the default implementations of
     * {@link RestWrapper#configurePersister(XStreamPersister, XStreamMessageConverter)} and
     * {@link RestHttpInputWrapper#configurePersister(XStreamPersister, XStreamMessageConverter)} constructed by
     * {@link #wrapObject(Object, Class)}, {@link #wrapList(Collection, Class)}, and
     * {@link #beforeBodyRead(HttpInputMessage, MethodParameter, Type, Class)}
     *
     * Override this method in subclasses to apply custom configuration.
     *
     * @param persister
     */
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) { }

    /**
     * Wraps the object being serialized in a {@link SimpleHash} template model.
     * <p>
     * The method {@link #wrapInternal(Map, SimpleHash, Object)} may be overridden to customize
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

        @SuppressWarnings("unchecked")
        @Override
        public TemplateModel wrap(Object object) throws TemplateModelException {
            if ( object instanceof SimpleHash) {
                return (SimpleHash) object;
            }
            if ( object instanceof Collection) {
                Collection c = (Collection) object;
                if (c.isEmpty() || clazz.isAssignableFrom(c.iterator().next().getClass())) {
                    SimpleHash hash = new SimpleHash();
                    hash.put("values", new CollectionModel(c, this));
                    setRequestInfo(hash);
                    wrapInternal(hash, (Collection<T>) object);
                    return hash;
                }
            }
            if ( object != null && clazz.isAssignableFrom( object.getClass() ) ) {
                HashMap<String, Object> map = new HashMap<String, Object>();

                ClassProperties cp = OwsUtils.getClassProperties(clazz);
                for ( String p : cp.properties() ) {
                    if ( "Class".equals( p ) ) continue;
                    Object value = null;
                    try {
                        value = OwsUtils.get(object, p);
                    } catch(Exception e) {
                        LOGGER.log(Level.WARNING, "Could not resolve property " + p + " of bean " + object, e);
                        value = "** Failed to retrieve value of property " + p + ". Error message is: " + e.getMessage() + "**";
                    }
                    if ( value == null ) {
                        value = "null";
                    }

                    map.put( Character.toLowerCase(p.charAt(0)) + p.substring(1), value.toString());

                }

                SimpleHash model = new SimpleHash();
                model.put( "properties", new MapModel(map, this) );
                model.put( "className", clazz.getSimpleName() );
                setRequestInfo(model);
                wrapInternal(map, model, (T) object);
                return model;
            }

            return super.wrap(object);
        }

        /**
         * Add {@link RequestInfo} to the freemarker model
         *
         * @param model
         * @throws TemplateModelException
         */
        protected void setRequestInfo(SimpleHash model) throws TemplateModelException {
            final RequestInfo requestInfo = RequestInfo.get();

            if (model.get("page") == null) {
                if (requestInfo != null) {
                    model.put("page", requestInfo);
                }
            }
        }

        /**
         * Template method to customize the returned template model.
         * Called in the case of a map model
         *
         * @param properties A map of properties obtained reflectively from the object being
         * serialized.
         * @param model The resulting template model.
         * @param object The object being serialized.
         */
        protected void wrapInternal(Map<String, Object> properties, SimpleHash model, T object) {
        }

        /**
         * Template method to customize the returned template model.
         * Called in the case of a list model
         *
         * @param model The resulting template model.
         * @param object The object being serialized.
         */
        protected void wrapInternal(SimpleHash model, Collection<T> object) {
        }
    }
}
