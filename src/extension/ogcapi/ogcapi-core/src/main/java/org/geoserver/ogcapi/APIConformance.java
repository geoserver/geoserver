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

    private final APIConformance parent;

    /** Conformance class identifier. */
    private final String id;

    /** Indicates standard approval level. */
    private final Level level;

    private final Type type;

    /** Bean property name. */
    private final String property;

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
        this(id, level, type, parent, id.substring(id.lastIndexOf('/') + 1));
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
        this.id = id;
        this.level = level;
        this.type = type;
        this.parent = parent;
        this.property = property;
    }

    public APIConformance extend(String id) {
        return new APIConformance(id, Level.STANDARD, Type.EXTENSION, this);
    }

    public APIConformance extend(String id, Level level) {
        return new APIConformance(id, level, Type.EXTENSION, this);
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
     * Conformance class standard level.
     *
     * @return conformance class standard level.
     */
    public Level getLevel() {
        return level;
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
