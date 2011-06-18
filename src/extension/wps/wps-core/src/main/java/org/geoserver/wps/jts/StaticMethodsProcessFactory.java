/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.jts;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * Grabbed from Geotools and generalized a bit, should go back into GeoTools once improved enough.
 * ProcessFactory for classes exposing simple processes as static methods
 * 
 * @since 2.7
 */
public class StaticMethodsProcessFactory<T> extends AnnotationDrivenProcessFactory {
    Class<T> targetClass;

    public StaticMethodsProcessFactory(InternationalString title, String namespace,
            Class<T> targetClass) {
        super(title, namespace);
        this.targetClass = targetClass;
    }

    /**
     * Finds the DescribeProcess description for the specified name
     * 
     * @param name
     * @return
     */
    protected DescribeProcess getProcessDescription(Name name) {
        Method method = method(name.getLocalPart());
        if (method == null) {
            return null;
        }
        DescribeProcess info = method.getAnnotation(DescribeProcess.class);
        return info;
    }

    public Method method(String name) {
        for (Method method : targetClass.getMethods()) {
            if (name.equalsIgnoreCase(method.getName())) {
                DescribeProcess dp = method.getAnnotation(DescribeProcess.class);
                if (dp != null) {
                    return method;
                }
            }
        }
        return null;
    }

    public Set<Name> getNames() {
        // look for the methods that have the DescribeProcess annotation. use
        // a linkedHashSet to make sure we don't report duplicate names
        Set<Name> names = new LinkedHashSet<Name>();
        for (Method method : targetClass.getMethods()) {
            DescribeProcess dp = method.getAnnotation(DescribeProcess.class);
            if (dp != null) {
                Name name = new NameImpl(namespace, method.getName());
                if (names.contains(name)) {
                    throw new IllegalStateException(targetClass.getName()
                            + " has two methods named " + method.getName()
                            + ", both annotated with DescribeProcess, this is an ambiguity. "
                            + "Please a different name");
                }
                names.add(name);
            }
        }
        return names;
    }

    @Override
    protected Object createProcessBean(Name name) {
        // they are all static methods
        return null;
    }

}
