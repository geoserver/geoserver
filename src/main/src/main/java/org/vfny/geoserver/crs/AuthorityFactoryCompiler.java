/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.codehaus.janino.SimpleCompiler;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.metadata.citation.Citation;

/**
 * Dynamically compiles UserAuthorityWKTFactory subclasses for different authority prefixes. This is needed to have
 * distinct class identities for each authority, as GeoTools uses class identity to collect factory, only once instance
 * per class is kept, and each CRSAuthorityFactory can only work with a single authority (ManyAuthorityFactory is a
 * specifically recognized exception, it's not usable in general terms, there can only be one of it in the system).
 */
final class AuthorityFactoryCompiler {

    record FactoryClasses(
            Class<? extends UserAuthorityWKTFactory> baseFactoryClass,
            Class<? extends UserAuthorityLongitudeFirstFactory> longitudeFirstFactoryClass) {}

    private static final Map<String, FactoryClasses> CLASS_CACHE = new ConcurrentHashMap<>();

    /**
     * Builds a UserAuthorityWKTFactory for the given authority prefix.
     *
     * @param authorityPrefix the authority prefix
     * @param citation the citation
     * @param resource the resource containing the CRS definitions
     * @return
     */
    public static UserAuthorityWKTFactory buildUserAuthority(
            String authorityPrefix, Citation citation, Resource resource) {
        try {
            FactoryClasses classes = CLASS_CACHE.computeIfAbsent(authorityPrefix, AuthorityFactoryCompiler::compile);
            Class<? extends UserAuthorityWKTFactory> cls = classes.baseFactoryClass;

            Constructor<? extends UserAuthorityWKTFactory> ctor = cls.getConstructor(Citation.class, Resource.class);

            return ctor.newInstance(citation, resource);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed creating factory for authority " + authorityPrefix, e);
        }
    }

    public static UserAuthorityLongitudeFirstFactory buildLongitudeFirstAuthority(
            String authorityPrefix, UserAuthorityWKTFactory backingStore) {
        try {
            FactoryClasses classes = CLASS_CACHE.computeIfAbsent(authorityPrefix, AuthorityFactoryCompiler::compile);
            Class<? extends UserAuthorityLongitudeFirstFactory> cls = classes.longitudeFirstFactoryClass;

            Constructor<? extends UserAuthorityLongitudeFirstFactory> ctor =
                    cls.getConstructor(UserAuthorityWKTFactory.class);

            return ctor.newInstance(backingStore);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed creating longitude-first factory for authority " + authorityPrefix, e);
        }
    }

    /**
     * Sanitizes an authority string to be used as a Java class name suffix.
     *
     * @param authority the authority string
     * @return a sanitized string safe for use as a class name suffix
     */
    private static String sanitizeForClassSuffix(String authority) {
        StringBuilder sb = new StringBuilder(authority.length());
        for (int i = 0; i < authority.length(); i++) {
            char ch = authority.charAt(i);
            sb.append(Character.isJavaIdentifierPart(ch) ? ch : '_');
        }
        return sb.toString();
    }

    private static FactoryClasses compile(String authorityPrefix) {
        Class<? extends UserAuthorityWKTFactory> baseClass = compileBaseFactory(authorityPrefix);
        Class<? extends UserAuthorityLongitudeFirstFactory> lonFirstClass =
                compileLongitudeFirstFactory(authorityPrefix);
        return new FactoryClasses(baseClass, lonFirstClass);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends UserAuthorityWKTFactory> compileBaseFactory(String authorityPrefix) {
        try {
            // Make a stable, valid Java identifier from the prefix
            String safe = sanitizeForClassSuffix(authorityPrefix);
            String pkg = UserAuthorityWKTFactory.class.getPackage().getName() + ".generated";
            String simpleName = "UserAuthorityWKTFactory__" + safe;
            String fqcn = pkg + "." + simpleName;

            // Minimal subclass: unique class identity + ctor forwarding to super
            String src = "package " + pkg + ";\n" + "public final class "
                    + simpleName + " extends " + UserAuthorityWKTFactory.class.getName() + " {\n" + "  public "
                    + simpleName + "(" + "    "
                    + Citation.class.getName() + " authority,\n" + "    "
                    + Resource.class.getName() + " properties\n" + "  ) {\n"
                    + "    super(authority, properties);\n"
                    + "  }\n"
                    + "}\n";

            SimpleCompiler sc = new SimpleCompiler();
            sc.setParentClassLoader(UserAuthorityWKTFactory.class.getClassLoader());
            sc.cook(src);

            Class<?> compiled = sc.getClassLoader().loadClass(fqcn);
            return (Class<? extends UserAuthorityWKTFactory>) compiled;
        } catch (Exception e) {
            throw new RuntimeException("Janino compile failed for authorityPrefix=" + authorityPrefix, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends UserAuthorityLongitudeFirstFactory> compileLongitudeFirstFactory(
            String authorityPrefix) {
        try {
            // Make a stable, valid Java identifier from the prefix
            String safe = sanitizeForClassSuffix(authorityPrefix);
            String pkg = UserAuthorityWKTFactory.class.getPackage().getName() + ".generated";
            String simpleName = "UserAuthorityLongitudeFirstFactory__" + safe;
            String fqcn = pkg + "." + simpleName;

            // Minimal subclass: unique class identity + ctor forwarding to super
            String src = "package " + pkg + ";\n" + "public final class "
                    + simpleName + " extends " + UserAuthorityLongitudeFirstFactory.class.getName() + " {\n"
                    + "  public "
                    + simpleName + "(" + "    "
                    + UserAuthorityWKTFactory.class.getName() + " backingStore) {\n"
                    + "    super(backingStore);\n"
                    + "  }\n"
                    + "}\n";

            SimpleCompiler sc = new SimpleCompiler();
            sc.setParentClassLoader(UserAuthorityLongitudeFirstFactory.class.getClassLoader());
            sc.cook(src);

            Class<?> compiled = sc.getClassLoader().loadClass(fqcn);
            return (Class<? extends UserAuthorityLongitudeFirstFactory>) compiled;
        } catch (Exception e) {
            throw new RuntimeException("Janino compile failed for authorityPrefix=" + authorityPrefix, e);
        }
    }

    private AuthorityFactoryCompiler() {}
}
