/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A XStream subclass allowing conversion of no class other than those explicitly registered using
 * the allowType* methods. To simplify the setup, it already allows the use of primitives, strings,
 * dates and collections
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecureXStream extends XStream {
    private static final String WHITELIST_KEY = "GEOSERVER_XSTREAM_WHITELIST";

    static final Logger LOGGER = Logging.getLogger(SecureXStream.class);

    public SecureXStream() {
        super();
        init();
    }

    public SecureXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(hierarchicalStreamDriver);
        init();
    }

    public SecureXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference,
            Mapper mapper,
            ConverterLookup converterLookup,
            ConverterRegistry converterRegistry) {
        super(
                reflectionProvider,
                driver,
                classLoaderReference,
                mapper,
                converterLookup,
                converterRegistry);
        init();
    }

    public SecureXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference,
            Mapper mapper) {
        super(reflectionProvider, driver, classLoaderReference, mapper);
        init();
    }

    public SecureXStream(
            ReflectionProvider reflectionProvider,
            HierarchicalStreamDriver driver,
            ClassLoaderReference classLoaderReference) {
        super(reflectionProvider, driver, classLoaderReference);
        init();
    }

    public SecureXStream(
            ReflectionProvider reflectionProvider,
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
        allowTypes(new Class[] {Mapper.Null.class});
        // allow primitives
        addPermission(new PrimitiveTypePermission());
        // and common non primitives
        allowTypes(
                new Class[] {
                    String.class, Date.class, java.sql.Date.class, Timestamp.class, Time.class
                });
        // allow common GeoTools types too
        allowTypeHierarchy(Filter.class);
        allowTypeHierarchy(NumberRange.class);
        allowTypeHierarchy(CoordinateReferenceSystem.class);
        allowTypeHierarchy(Name.class);
        allowTypes(new Class[] {Version.class, SimpleInternationalString.class});
        // common collection types
        allowTypes(
                new Class[] {
                    TreeSet.class,
                    SortedSet.class,
                    Set.class,
                    HashSet.class,
                    LinkedHashSet.class,
                    List.class,
                    ArrayList.class,
                    CopyOnWriteArrayList.class,
                    Map.class,
                    HashMap.class,
                    TreeMap.class,
                    ConcurrentHashMap.class,
                });

        // Allow classes from user defined whitelist
        String whitelistProp = GeoServerExtensions.getProperty(WHITELIST_KEY);
        if (whitelistProp != null) {
            String[] wildcards = whitelistProp.split("\\s+|(\\s*;\\s*)");
            this.allowTypesByWildcard(wildcards);
        }
    }

    @Override
    protected MapperWrapper wrapMapper(MapperWrapper next) {
        return new DetailedSecurityExceptionWrapper(next);
    }

    /**
     * A wrapper that adds instructions on what to do when a class was not part of the whitelist
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class DetailedSecurityExceptionWrapper extends MapperWrapper {

        public DetailedSecurityExceptionWrapper(Mapper wrapped) {
            super(wrapped);
        }

        @Override
        public Class realClass(String elementName) {
            try {
                return super.realClass(elementName);
            } catch (ForbiddenClassException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Class {0} is not whitelisted for XML parsing. \n");
                sb.append(
                                "This is done to prevent Remote Code Execution attacks, but it might be \n")
                        .append(
                                "you need this class to be authorized for GeoServer to actually work\n");
                sb.append("If you are a user, you can set a variable named ")
                        .append(WHITELIST_KEY)
                        .append("\n")
                        .append(
                                "  with a semicolon separated list of fully qualified names, or patterns\n")
                        .append(
                                "  to match several classes.The variable can be set as a system variable,\n")
                        .append(
                                "  an environment variable, or a servlet context variable, just like\n")
                        .append("  GEOSERVER_DATA_DIR.\n")
                        .append(
                                "  For example, in order to authorize the org.geoserver.Foo class,\n")
                        .append(
                                "  plus any class in the org.geoserver.custom package, one could set\n")
                        .append("  a system variable: \n")
                        .append("  -D")
                        .append(WHITELIST_KEY)
                        .append("=org.geoserver.Foo;org.geoserver.custom.**\n");
                sb.append(
                                "If instead you are a developer, you can call allowTypes/allowTypeHierarchy against\n")
                        .append("  the XStream used for serialization by rolling a custom\n")
                        .append(
                                "  XStreamPersisterInitializer or customizing your XStreamServiceLoader.");
                LOGGER.log(Level.SEVERE, sb.toString(), e.getMessage());

                throw new ForbiddenClassExceptionEx(
                        "Unauthorized class found, see logs for more details on how to handle it: "
                                + e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Just to have a recognizable class for tests
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class ForbiddenClassExceptionEx extends RuntimeException {

        public ForbiddenClassExceptionEx(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
