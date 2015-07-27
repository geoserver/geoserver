/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.Version;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

/**
 * A XStream subclass allowing conversion of no class other than those explicitly registered using
 * the allowType* methods. To simplify the setup, it already allows the use of primitives, strings,
 * dates and collections
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SecureXStream extends XStream {

    public SecureXStream() {
        super();
        init();
    }

    public SecureXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(hierarchicalStreamDriver);
        init();
    }

    public SecureXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference, Mapper mapper,
            ConverterLookup converterLookup, ConverterRegistry converterRegistry) {
        super(reflectionProvider, driver, classLoaderReference, mapper, converterLookup,
                converterRegistry);
        init();
    }

    public SecureXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference, Mapper mapper) {
        super(reflectionProvider, driver, classLoaderReference, mapper);
        init();
    }

    public SecureXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference) {
        super(reflectionProvider, driver, classLoaderReference);
        init();
    }

    public SecureXStream(ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(reflectionProvider, hierarchicalStreamDriver);
        init();
    }

    public SecureXStream(ReflectionProvider reflectionProvider) {
        super(reflectionProvider);
        init();
    }

    private void init() {
        // by default, convert nothing
        addPermission(NoTypePermission.NONE);

        // the placeholder for null values
        allowTypes(new Class[] { Mapper.Null.class });
        // allow primitives
        addPermission(new PrimitiveTypePermission());
        // and common non primitives
        allowTypes(new Class[] { String.class, Date.class, java.sql.Date.class, Timestamp.class,
                Time.class });
        // allow common GeoTools types too
        allowTypeHierarchy(Filter.class);
        allowTypeHierarchy(NumberRange.class);
        allowTypeHierarchy(CoordinateReferenceSystem.class);
        allowTypeHierarchy(Name.class);
        allowTypes(new Class[] { Version.class, SimpleInternationalString.class });
        // common collection types
        allowTypes(new Class[] { TreeSet.class, SortedSet.class, Set.class, HashSet.class,
                List.class, ArrayList.class, CopyOnWriteArrayList.class, Map.class, HashMap.class,
                ConcurrentHashMap.class, });
    }



}
