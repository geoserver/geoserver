/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.MapModel;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;

/**
 * Wraps the object being serialized in a {@link SimpleHash} template model.
 *
 * <p>The method {@link #wrapInternal(Map, SimpleHash, Object)} may be overridden to customize the
 * returned model.
 */
public class ObjectToMapWrapper<T> extends BeansWrapper {
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.rest");

    /** The class of object being serialized. */
    Class<T> clazz;

    Collection<Class> classesToExpand;

    /** Constructs an ObjectToMapWrapper for the provided clazz. */
    public ObjectToMapWrapper(Class<T> clazz) {
        this(clazz, Collections.EMPTY_LIST);
    }

    /**
     * Constructs an ObjectToMapWrapper for the provided clazz. Any child properties that match
     * classesToExpand will be unwrapped to a map
     */
    public ObjectToMapWrapper(Class<T> clazz, Collection<Class> classesToExpand) {
        this.clazz = clazz;
        this.classesToExpand = classesToExpand;
    }

    /**
     * Constructs a {@link SimpleHash} representing the passed object.
     *
     * <p>If the object is already a SimpleHash, it is returned. If the object is a {@link
     * Collection}, with contents matching {@link #clazz}, a SimpleHash with a single entry is
     * returned:
     *
     * <p>"values", containing the collection, as a {@link CollectionModel}
     *
     * <p>If the object is an {@link Object}, that matches {@link #clazz}, a SimpleHash with a two
     * entries is returned:
     *
     * <p>"properties", containing a {@link MapModel} representing the object. Map entries are
     * populated using reflection to get the property names and values. See {@link
     * OwsUtils#get(Object, String)} for more details. If any values have a class assignable to any
     * class included in {@link #classesToExpand}, those values are likewise extracted into a map.
     * Otherwise the toString method of that object is called.
     *
     * <p>If none of the above give a result, {@link BeansWrapper#wrap(Object)} is returned.
     *
     * @param object Object to wrap
     * @return A {@link SimpleHash} representing the passed object.
     */
    @SuppressWarnings("unchecked")
    @Override
    public TemplateModel wrap(Object object) throws TemplateModelException {
        if (object instanceof SimpleHash) {
            return (SimpleHash) object;
        }
        if (object instanceof Collection) {
            Collection c = (Collection) object;
            if (c.isEmpty() || clazz.isAssignableFrom(c.iterator().next().getClass())) {
                SimpleHash hash = new SimpleHash();
                hash.put("values", new CollectionModel(c, this));
                setRequestInfo(hash);
                wrapInternal(hash, (Collection<T>) object);
                return hash;
            }
        }
        if (object != null && clazz.isAssignableFrom(object.getClass())) {
            Map<String, Object> map = objectToMap(object, clazz);

            SimpleHash model = new SimpleHash();
            model.put("properties", new MapModel(map, this));
            model.put("className", clazz.getSimpleName());
            setRequestInfo(model);
            wrapInternal(map, model, (T) object);
            return model;
        }

        return super.wrap(object);
    }

    /**
     * Converts the provided object to a map using reflection on on clazz.
     *
     * <p>If any values have a class assignable to any class included in {@link #classesToExpand},
     * those values are likewise extracted into a map.
     *
     * @param object Object to convert.
     * @param clazz The advertized class of the object, from which the map keys are generated.
     */
    protected Map<String, Object> objectToMap(Object object, Class clazz) {
        HashMap<String, Object> map = new HashMap<>();

        ClassProperties cp = OwsUtils.getClassProperties(clazz);
        for (String p : cp.properties()) {
            if ("Class".equals(p)) continue;
            Object value;
            try {
                value = OwsUtils.get(object, p);
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING, "Could not resolve property " + p + " of bean " + object, e);
                value =
                        "** Failed to retrieve value of property "
                                + p
                                + ". Error message is: "
                                + e.getMessage()
                                + "**";
            }
            if (value == null) {
                value = "null";
            }
            String key = Character.toLowerCase(p.charAt(0)) + p.substring(1);
            Class valueClass = getClassForUnwrapping(value);
            if (value instanceof Collection) {
                List values = new ArrayList();
                for (Object o : (Collection) value) {
                    valueClass = getClassForUnwrapping(o);
                    if (valueClass == null) {
                        values.add(o == null ? "" : o.toString());
                    } else {
                        values.add(objectToMap(o, valueClass));
                    }
                }
                map.put(key, new CollectionModel(values, this));
            } else if (valueClass == null) {
                map.put(key, value.toString());
            } else {
                map.put(key, objectToMap(value, valueClass));
            }
        }
        return map;
    }

    private Class getClassForUnwrapping(Object o) {
        for (Class clazz : classesToExpand) {
            if (clazz.isAssignableFrom(o.getClass())) {
                return clazz;
            }
        }
        return null;
    }

    /** Add {@link RequestInfo} to the freemarker model */
    protected void setRequestInfo(SimpleHash model) throws TemplateModelException {
        final RequestInfo requestInfo = RequestInfo.get();

        if (model.get("page") == null) {
            if (requestInfo != null) {
                model.put("page", requestInfo);
            }
        }
    }

    /**
     * Template method to customize the returned template model. Called in the case of a map model
     *
     * @param properties A map of properties obtained reflectively from the object being serialized.
     * @param model The resulting template model.
     * @param object The object being serialized.
     */
    protected void wrapInternal(Map<String, Object> properties, SimpleHash model, T object) {}

    /**
     * Template method to customize the returned template model. Called in the case of a list model
     *
     * @param model The resulting template model.
     * @param object The object being serialized.
     */
    protected void wrapInternal(SimpleHash model, Collection<T> object) {}
}
