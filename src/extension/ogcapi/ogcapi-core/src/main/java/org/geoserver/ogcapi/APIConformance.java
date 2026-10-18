/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.Serializable;
import java.util.Objects;

/**
 * Service capability or feature, identified by conformance class.
 *
 * <p>OGCAPI Web Services are defined with core functionality, strictly extended with additional, optional,
 * functionality identified by "conformance class".
 *
 * <p>By comparison OGC Open Web Services can be extended using application profiles with additional, optional,
 * functionality.
 */
@SuppressWarnings("serial")
public class APIConformance implements Serializable {

    /** There are three levels of standard. */
    public enum Level {
        /**
         * Draft developed by communities external to the OGC or other official organization.
         *
         * <p>GeoServer community modules are community draft standards under development.
         */
        COMMUNITY_DRAFT(false, false),
        /**
         * Developed by communities external to the OGC or other official organization.
         *
         * <p>GeoServer vendor extensions are considered community standards.
         */
        COMMUNITY_STANDARD(true, false),

        /**
         * Draft standard being developed by OGC membership or other official organization.
         *
         * <p>This protocol is under active development, often seeking funding and feedback. GeoSever community modules
         * are used to explore draft standards.
         *
         * <p>This functionality is opt-in and should not be enabled by default.
         */
        DRAFT_STANDARD(false, true),

        /**
         * Mature standard, however the implementation is still under development.
         *
         * <p>Does not yet pass CITE certification associated with standard.
         */
        IMPLEMENTING(false, true),

        /**
         * Mature standard, stable and ready for use.
         *
         * <p>A finalized standard, no longer subject to breaking changes, is required for a GeoServer extension to be
         * published. This functionality is stable and enabled by default.
         */
        STANDARD(true, true),

        /**
         * Standards are dynamic and are retired when they are no longer in use.
         *
         * <p>Retired standards are not enabled by default, but may still be enabled if you made use of them in a
         * previous version of GeoServer.
         */
        RETIRED_STANDARD(true, false);

        /** Standard is currently endorsed by the OGC or other official organization. */
        private final boolean endorsed;
        /** Standard is stable and no longer subject to change. */
        private final boolean stable;

        Level(boolean stable, boolean endorsed) {
            this.endorsed = endorsed;
            this.stable = stable;
        }

        /**
         * Standard is currently endorsed by the OGC or other official organization.
         *
         * @return true if the standard is officially endorsed, false otherwise.
         */
        public boolean isEndorsed() {
            return endorsed;
        }

        /**
         * Standard is stable and no longer subject to change.
         *
         * @return true if the standard is stable and no longer subject, false if the standard is experimental and not
         *     yet finalized.
         */
        public boolean isStable() {
            return stable;
        }
    }

    public enum Type {
        CORE,
        EXTENSION
    }

    /** Parent conformance class, {@code null} if not an extension. */
    private final APIConformance parent;

    /** Conformance class identifier. */
    private final String id;

    /** Indicates standard approval level. */
    private final Level level;

    /** Indicates conformance class type, either core or extension. */
    private final Type type;

    /** Bean property name. */
    private final String property;

    /** Built-in status, {@code null} if configurable. */
    private final Boolean builtIn;

    /**
     * Conformance class declaration, defaulting to APPROVED.
     *
     * @param id conformance class
     */
    public APIConformance(String id) {
        this(id, Level.STANDARD);
    }

    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param level standard approval status
     */
    public APIConformance(String id, Level level) {
        this(id, level, Type.EXTENSION, null);
    }

    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param level standard approval status
     * @param type conformance class type
     */
    public APIConformance(String id, Level level, Type type) {
        this(id, level, type, null);
    }

    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param level standard approval status
     * @param property storage key
     */
    public APIConformance(String id, Level level, String property) {
        this(id, level, Type.EXTENSION, null, property);
    }

    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param level standard approval status
     * @param type conformance class type
     * @param parent parent conformance class (if this is an extension)
     */
    public APIConformance(String id, Level level, Type type, APIConformance parent) {
        this(id, level, type, parent, propertyName(id));
    }
    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param level standard approval status
     * @param type conformance class type
     * @param parent parent conformance class (if this is an extension)
     * @param property bean property name
     */
    public APIConformance(String id, Level level, Type type, APIConformance parent, String property) {
        this(id, level, type, parent, property, null);
    }

    private APIConformance(String id, Level level, Type type, APIConformance parent, String property, Boolean builtIn) {
        this.id = id;
        this.level = level;
        this.type = type;
        this.parent = parent;
        this.property = property;
        this.builtIn = builtIn;
    }

    /**
     * Extension of this conformance class.
     *
     * @param id conformance class, or a name resolved against this conformance class identifier
     * @return extension conformance class declaration
     * @see #extend(String, Level)
     */
    public APIConformance extend(String id) {
        return extend(id, Level.STANDARD);
    }

    /**
     * Extension of this conformance class.
     *
     * <p>A name such as {@code "html"} is resolved against this conformance class identifier, so extending
     * {@code https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/core} produces
     * {@code https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/html}.
     *
     * @param id conformance class, or a name resolved against this conformance class identifier
     * @param level standard approval status
     * @return extension conformance class declaration
     */
    public APIConformance extend(String id, Level level) {
        String extensionId = id.contains(":") ? id : this.id.substring(0, this.id.lastIndexOf('/') + 1) + id;
        return new APIConformance(extensionId, level, Type.EXTENSION, this);
    }

    /**
     * Conformance class declaration that is not configurable.
     *
     * <p>Use {@code true} for functionality that is always available and cannot be disabled, or {@code false} to
     * document functionality that is not implemented.
     *
     * @param implemented {@code true} if always enabled, {@code false} if not implemented
     * @return built-in conformance class declaration
     */
    public APIConformance builtIn(boolean implemented) {
        return new APIConformance(id, level, type, parent, property, implemented);
    }

    /**
     * Conformance class declaration with the provided bean property name.
     *
     * <p>Use when the property cannot be derived from the conformance class identifier.
     *
     * @param property bean property name
     * @return conformance class declaration using the provided property
     */
    public APIConformance property(String property) {
        return new APIConformance(id, level, type, parent, property, builtIn);
    }

    /**
     * ServiceConformance conformance identifier.
     *
     * @return service module conformance identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Derive bean property name from the last segment of the conformance class identifier.
     *
     * <p>Hyphenated segments are converted to camel case, so {@code spatial-subsetting} becomes
     * {@code spatialSubsetting}.
     *
     * @param id conformance class
     * @return bean property name
     */
    static String propertyName(String id) {
        String[] words = id.substring(id.lastIndexOf('/') + 1).split("-");
        StringBuilder property = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                property.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
            }
        }
        return property.toString();
    }

    /**
     * Recommended storage key.
     *
     * <p>To avoid confusion the recommended storage key is derived from the conformance class identifier. This may be
     * overriden by the constructor.
     *
     * @return recommended storage key.
     */
    public String getProperty() {
        return property;
    }

    /**
     * Built-in conformance status, not subject to configuration.
     *
     * @return {@code true} if always enabled, {@code false} if not implemented, or {@code null} if configurable.
     */
    public Boolean getBuiltIn() {
        return builtIn;
    }

    /**
     * Conformance class can be enabled or disabled by configuration.
     *
     * @return {@code true} if configurable, {@code false} for built-in conformance.
     */
    public boolean isConfigurable() {
        return builtIn == null;
    }

    /**
     * Conformance class standard level.
     *
     * @return conformance class standard level.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Conformance class type, either core or extension.
     *
     * @return conformance class type.
     */
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        APIConformance serviceModule = (APIConformance) o;
        return Objects.equals(id, serviceModule.id);
    }

    @Override
    public String toString() {
        return "APIConformance " + property + " ( " + id + " " + level + " )";
    }
}
