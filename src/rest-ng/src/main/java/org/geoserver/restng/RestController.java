package org.geoserver.restng;

import freemarker.core.ParseException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.MapModel;
import freemarker.template.*;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.restng.catalog.wrapper.XStreamListWrapper;
import org.geoserver.restng.wrapper.FreemarkerConfigurationWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for all rest-ng controllers
 *
 * Provided various utilities for dealing with the {@link org.geoserver.restng.converters.FreemarkerHTMLMessageConverter}
 */
public class RestController {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.restng");

    /**
     * Default encoding for the freemarker {@link Configuration}
     */
    protected String encoding = "UTF-8";

    /**
     * Name of the folder containing freemarker templates
     */
    protected String pathPrefix = "";

    /**
     * Wrapes the passed object in a {@link FreemarkerConfigurationWrapper}
     *
     * @param object
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> FreemarkerConfigurationWrapper toFreemarkerMap(Object object) {
        return toFreemarkerMap((T) object, (Class<T>) object.getClass());
    }

    /**
     * Wraps the passed object in a {@link FreemarkerConfigurationWrapper}
     *
     * @param object The object to wrap
     * @param clazz The advertised class to use for the object
     * @return
     */
    protected <T> FreemarkerConfigurationWrapper toFreemarkerMap(T object, Class<T> clazz) {
        return new FreemarkerConfigurationWrapper(object, getTemplate(object, clazz));
    }

    /**
     * Wraps the passed collection in a {@link FreemarkerConfigurationWrapper}
     *
     * @param list The collection to wrap
     * @return
     */
    @SuppressWarnings("unchecked")
    protected FreemarkerConfigurationWrapper toFreemarkerList(Collection list) {
        if (!list.isEmpty()) {
            Object o = list.iterator().next();
            return toFreemarkerList(list, o.getClass());
        }
        return toFreemarkerList(list, Object.class);
    }

    /**
     * Wraps the passed collection in a {@link FreemarkerConfigurationWrapper}
     *
     * @param list The collection to wrap
     * @param clazz The advertised class to use for the collection contents
     * @return
     */
    protected <T> FreemarkerConfigurationWrapper toFreemarkerList(Collection<T> list, Class<T> clazz) {
        return new FreemarkerConfigurationWrapper(list, getTemplate(list, clazz));
    }

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
        final RequestInfo requestInfo = (RequestInfo) RequestContextHolder.getRequestAttributes().getAttribute( RequestInfo.KEY, RequestAttributes.SCOPE_REQUEST );

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
     * Wraps the passed collection in a {@link XStreamListWrapper}
     *
     * @param list The collection to wrap
     * @param clazz The advertised class to use for the collection contents
     * @return
     */
    protected <T> XStreamListWrapper<T> toXStreamList(Collection<T> list, Class<T> clazz) {
        return new XStreamListWrapper<>(list, clazz);
    }

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
            final RequestInfo requestInfo = (RequestInfo) RequestContextHolder.getRequestAttributes().getAttribute( RequestInfo.KEY, RequestAttributes.SCOPE_REQUEST );

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
