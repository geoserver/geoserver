/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

public class AnnotatedBeanProcessFactory extends AnnotationDrivenProcessFactory {
    Map<String, Class> classMap;

    public AnnotatedBeanProcessFactory(InternationalString title, String namespace,
            Class... beanClasses) {
        super(title, namespace);
        classMap = new HashMap<String, Class>();
        for (Class c : beanClasses) {
            String name = c.getSimpleName();
            if (name.endsWith("Process")) {
                name = name.substring(0, name.indexOf("Process"));
            }
            classMap.put(name, c);
        }
    }

    @Override
    protected DescribeProcess getProcessDescription(Name name) {
        Class c = classMap.get(name.getLocalPart());
        if (c == null) {
            return null;
        } else {
            return (DescribeProcess) c.getAnnotation(DescribeProcess.class);
        }
    }

    @Override
    protected Method method(String className) {
        Class c = classMap.get(className);
        if (c != null) {
            for (Method m : c.getMethods()) {
                if ("execute".equals(m.getName())) {
                    return m;
                }
            }
        }
        return null;
    }

    public Set<Name> getNames() {
        Set<Name> result = new LinkedHashSet<Name>();
        List<String> names = new ArrayList<String>(classMap.keySet());
        Collections.sort(names);
        for (String name : names) {
            result.add(new NameImpl(namespace, name));
        }
        return result;
    }

    protected Object createProcessBean(Name name) {
        try {
            return classMap.get(name.getLocalPart()).newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
