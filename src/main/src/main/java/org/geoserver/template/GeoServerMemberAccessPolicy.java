/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import static org.geoserver.template.TemplateUtils.FM_VERSION;

import com.google.common.base.Splitter;
import freemarker.ext.beans.ClassMemberAccessPolicy;
import freemarker.ext.beans.DefaultMemberAccessPolicy;
import freemarker.ext.beans.MemberAccessPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * A custom member access policy implementation that provides stricter restrictions that FreeMarker does by default for
 * what fields and methods can be accessed and allows an administrator to set system properties to control these
 * restrictions. A field/method is only exposed if the class of the template object and the class of the field or the
 * method's return type are in the list of allowed classes/packages and not in the list of blocked classes/packages.
 *
 * <p>An additional property controls whether to allow access to only getter methods or to all methods. While the allow
 * and block lists should be sufficient to ensure that templates are safe, limiting templates to only getter methods
 * will help prevent new vulnerabilities from being introduced as new capabilities are added to GeoServer's API.
 */
public final class GeoServerMemberAccessPolicy implements MemberAccessPolicy {

    private static final Logger LOGGER = Logging.getLogger(GeoServerMemberAccessPolicy.class);

    /** System property to add classes and packages to allow access in FreeMarker templates. */
    public static final String FREEMARKER_ALLOW_LIST = "GEOSERVER_FREEMARKER_ALLOW_LIST";

    /** System property to add classes and packages to block access in FreeMarker templates. */
    public static final String FREEMARKER_BLOCK_LIST = "GEOSERVER_FREEMARKER_BLOCK_LIST";

    /** System property to restrict FreeMarker templates to only access getter methods. */
    public static final String FREEMARKER_API_EXPOSED = "GEOSERVER_FREEMARKER_API_EXPOSED";

    /** The default member access policy */
    private static final DefaultMemberAccessPolicy DEFAULT_POLICY = DefaultMemberAccessPolicy.getInstance(FM_VERSION);

    /** Default list of classes and packages to allow */
    private static final List<Object> DEFAULT_ALLOW = List.of(
            "java.",
            javax.xml.namespace.QName.class,
            "net.opengis.",
            "org.geoserver.",
            "org.geotools.",
            "org.locationtech.jts.geom.");

    /** Default list of classes and packages to block */
    private static final List<Object> DEFAULT_BLOCK = List.of(
            java.io.InputStream.class,
            java.io.OutputStream.class,
            java.lang.Class.class,
            java.lang.ClassLoader.class,
            java.lang.reflect.InvocationHandler.class,
            "java.lang.reflect.",
            "java.security.",
            org.geoserver.catalog.Catalog.class,
            org.geoserver.platform.resource.ResourceStore.class,
            org.geotools.api.data.DataAccess.class,
            org.geotools.api.coverage.grid.GridCoverageReader.class,
            org.geotools.api.coverage.grid.GridCoverageWriter.class);

    /** Block all static field/method access by default */
    private static final Predicate<Class<?>> DEFAULT_STATIC_ACCESS = clazz -> false;

    /**
     * Policy that uses the system property to determine whether to allow access to all methods or only getter methods
     * that pass other safety checks
     */
    public static final GeoServerMemberAccessPolicy DEFAULT_ACCESS = new GeoServerMemberAccessPolicy(null, null, null);

    /**
     * Policy that that ignores the system property and always allow access to all methods that pass other safety checks
     */
    public static final GeoServerMemberAccessPolicy FULL_ACCESS = new GeoServerMemberAccessPolicy(true, null, null);

    /**
     * Policy that that ignores the system property and always allow access to only getter methods that pass other
     * safety checks
     */
    public static final GeoServerMemberAccessPolicy LIMIT_ACCESS = new GeoServerMemberAccessPolicy(false, null, null);

    /** Whether to override the system property that restricts access to only getter methods */
    private final Boolean forceApiExposed;

    /** Additional classes/packages to add to the allow list */
    private final List<Object> forceAllowList;

    /** Predicate that determines if static field/method access is allowed for a certain class */
    private final Predicate<Class<?>> staticAccess;

    /** List of classes and packages to allow */
    private volatile List<Object> allowList = null;

    /** List of classes and packages to block */
    private volatile List<Object> blockList = null;

    /** Whether to restrict access to only getter methods */
    private volatile Boolean apiExposed = null;

    private GeoServerMemberAccessPolicy(Boolean apiExposed, List<Object> allowList, Predicate<Class<?>> staticAccess) {
        this.forceApiExposed = apiExposed;
        this.apiExposed = this.forceApiExposed;
        this.forceAllowList = allowList != null ? allowList : List.of();
        this.staticAccess = staticAccess != null ? staticAccess : DEFAULT_STATIC_ACCESS;
    }

    /** Creates a new policy with the provided classes/packages added to the allow list */
    public GeoServerMemberAccessPolicy withAllowList(Object... allowList) {
        return new GeoServerMemberAccessPolicy(this.forceApiExposed, List.of(allowList), this.staticAccess);
    }

    /** Creates a new policy with the provided static access controls */
    public GeoServerMemberAccessPolicy withStaticAccess(Predicate<Class<?>> staticAccess) {
        return new GeoServerMemberAccessPolicy(this.forceApiExposed, this.forceAllowList, staticAccess);
    }

    @Override
    public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
        return new GeoServerClassMemberAccessPolicy(contextClass);
    }

    @Override
    public boolean isToStringAlwaysExposed() {
        return true;
    }

    /**
     * Resets the fields so that they will be reloaded from the system properties the next time they are needed. This is
     * intended for unit tests only.
     */
    public synchronized void reset() {
        this.allowList = null;
        this.blockList = null;
        this.apiExposed = this.forceApiExposed;
    }

    /**
     * Gets the list of classes and packages to allow, initializing it from the system property the first time it is
     * called
     */
    private List<Object> getAllowList() {
        if (this.allowList == null) {
            init();
        }
        return this.allowList;
    }

    /**
     * Gets the list of classes and packages to block, initializing it from the system property the first time it is
     * called
     */
    private List<Object> getBlockList() {
        if (this.blockList == null) {
            init();
        }
        return this.blockList;
    }

    /**
     * Checks whether to allow full API access or restrict access to getter methods only, initializing it from the
     * system property the first time it is called.
     */
    private boolean isApiExposed() {
        if (this.apiExposed == null) {
            init();
        }
        return this.apiExposed;
    }

    /** Initializes the fields from the system properties. */
    private synchronized void init() {
        if (this.allowList == null) {
            List<Object> list = DEFAULT_ALLOW;
            if (!this.forceAllowList.isEmpty()) {
                list = Stream.concat(list.stream(), this.forceAllowList.stream())
                        .collect(Collectors.toUnmodifiableList());
            }
            this.allowList = parseList(FREEMARKER_ALLOW_LIST, list);
        }
        if (this.blockList == null) {
            this.blockList = parseList(FREEMARKER_BLOCK_LIST, DEFAULT_BLOCK);
        }
        if (this.apiExposed == null) {
            this.apiExposed = Boolean.parseBoolean(GeoServerExtensions.getProperty(FREEMARKER_API_EXPOSED));
        }
    }

    /**
     * Looks up the value of the system property with the provided key and parses it as a comma separated list, check if
     * each string is a valid class name or otherwise assumes that it is a package name prefix, and appends the parsed
     * values to the list of default values
     */
    private static List<Object> parseList(String key, List<Object> defaults) {
        String value = GeoServerExtensions.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaults;
        }
        Stream<Object> stream = Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .splitToStream(value)
                .map(name -> {
                    try {
                        return Class.forName(name);
                    } catch (ClassNotFoundException e) {
                        return name;
                    }
                });
        return Stream.concat(defaults.stream(), stream).collect(Collectors.toUnmodifiableList());
    }

    private final class GeoServerClassMemberAccessPolicy implements ClassMemberAccessPolicy {

        /** The exact class of the current object */
        private final Class<?> contextClass;

        /** Whether the current class is allowed */
        private final boolean isContextClassAllowed;

        /** The default member access policy for the current class */
        private final ClassMemberAccessPolicy defaultPolicy;

        private GeoServerClassMemberAccessPolicy(Class<?> contextClass) {
            this.contextClass = contextClass;
            this.isContextClassAllowed = isClassAllowed(contextClass);
            this.defaultPolicy = DEFAULT_POLICY.forClass(contextClass);
        }

        @Override
        public boolean isConstructorExposed(Constructor<?> constructor) {
            return false;
        }

        @Override
        public boolean isFieldExposed(Field field) {
            // check if the default policy exposes the field
            if (!this.defaultPolicy.isFieldExposed(field)) {
                return false;
            }
            // GeoServer has never enabled direct access to instance fields so this will only be
            // called on "public static" fields for classes that have their static model exposed.
            // Check if the current class and the class of the field are allowed, statics are
            // exposed for the current class, and the field is also final (e.g., Math.PI).
            boolean exposed = this.isContextClassAllowed
                    && Modifier.isFinal(field.getModifiers())
                    && staticAccess.test(this.contextClass)
                    && isClassAllowed(field.getType());
            if (!exposed) {
                LOGGER.finer(() -> "Blocked access to field " + this.contextClass.getName() + "." + field.getName());
            }
            return exposed;
        }

        @Override
        public boolean isMethodExposed(Method method) {
            if (!this.defaultPolicy.isMethodExposed(method)) {
                // check if the default policy exposes the method
                return false;
            } else if (method.getParameterCount() == 0 && method.getName().equals("toString")) {
                // allow toString() for backwards-compatibility even though templates shouldn't call it directly
                return true;
            }
            boolean exposed = false;
            // check if the return type is allowed
            if (isClassAllowed(method.getReturnType())) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    // check if the current class is allowed for instance methods and check if the
                    // the method is a getter method
                    exposed = this.isContextClassAllowed && checkGetterMethod(method);
                } else {
                    // check if statics are exposed for the current class
                    exposed = staticAccess.test(this.contextClass);
                }
            }
            if (!exposed) {
                LOGGER.finer(() -> "Blocked access to method " + this.contextClass.getName() + "." + method.getName());
            }
            return exposed;
        }

        /**
         * Check if the system property restricts access to only getter methods and if the restriction is enabled, check
         * if the method is a zero-argument method with a name that equals "toString", a name that starts with "get" and
         * has a non-void return type or a name the starts with "is" and has a boolean return type.
         */
        private boolean checkGetterMethod(Method method) {
            if (isApiExposed()) {
                // system property enables access to all methods
                return true;
            }
            boolean exposed = false;
            if (method.getParameterCount() == 0) {
                String name = method.getName();
                Class<?> type = method.getReturnType();
                if (name.startsWith("get") && name.length() > 3) {
                    exposed = !type.equals(void.class);
                } else if (name.startsWith("is") && name.length() > 2) {
                    exposed = type.equals(boolean.class) || type.equals(Boolean.class);
                }
            }
            return exposed;
        }

        /**
         * Checks if the provided class is either a proxy of a GeoServer interface, a primitive type or it is in the
         * allow list and not in the block list
         */
        private boolean isClassAllowed(Class<?> clazz) {
            if (clazz.equals(void.class)) {
                return false;
            } else if (Proxy.isProxyClass(clazz)) {
                List<Class<?>> interfaces = Stream.of(clazz.getInterfaces())
                        .filter(c -> c.getName().startsWith("org.geoserver."))
                        .collect(Collectors.toUnmodifiableList());
                return !interfaces.isEmpty() && interfaces.stream().noneMatch(c -> matchesAny(c, getBlockList()));
            }
            // unwrap array types until getting a non-array type
            Class<?> actual = clazz;
            while (actual.isArray()) {
                actual = actual.getComponentType();
            }
            return actual.isPrimitive() || (matchesAny(actual, getAllowList()) && !matchesAny(actual, getBlockList()));
        }

        /** Checks if the provided class matches any of the classes in the list */
        private boolean matchesAny(Class<?> clazz, List<Object> list) {
            String name = clazz.getName();
            return list.stream()
                    .anyMatch(object -> object instanceof Class<?> c
                            ? c.isAssignableFrom(clazz)
                            : name.startsWith((String) object));
        }
    }
}
