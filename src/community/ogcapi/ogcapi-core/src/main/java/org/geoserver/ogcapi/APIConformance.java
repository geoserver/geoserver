package org.geoserver.ogcapi;

import java.util.Objects;

/**
 * Service capability or feature, identified by conformance class.
 *
 * OGCAPI Web Services are defined with core functionality, strictly extended with additional, optional, functionality
 * identified by "conformance class".
 *
 * By comparision OGC Open Web Services can be extended using application profiles with additional, optional, functionality.
 */
public class APIConformance {

    /**
     * There are three levels of standard.
     */
    public enum Level {
        /**
         * Developed by communities external to the OGC or other official organization.
         *
         * GeoServer vendor extensions are considered community standards.
         */
        COMMUNITY_STANDARD(true,false),

        /**
         * Draft standard being developed by OGC membership or other official organization.
         * <p>
         * This protocol is under active development, often seeking funding and feedback.
         * GeoSever community modules are used to explore draft standards.
         * </p>
         * <p>
         * This functionality is opt-in and should not be enabled by default.
         */
        DRAFT_STANDARD(false,true),

        /**
         * Mature standard, stable and ready for use.
         * <p>
         * A finalized standard, no longer subject to breaking changes, is required for
         * a GeoServer extension to be published.
         * </p>
         * This functionality is stable and enabled by default.
         */
        STANDARD(true,true),

        /**
         * Standards are dynamic and are retired when they are no longer in use.
         *
         * Retired standards are not enabled by default, but may still be enabled if you
         * made use of them in a previous version of GeoServer.
         */
        RETIRED_STANDARD(true, false);

        /**
         * Standard is currently endorsed by the OGC or other official organization.
         */
        private final boolean endorsed;
        /**
         * Standard is stable and no longer subject to change.
         */
        private final boolean stable;

        Level(boolean stable,boolean endorsed) {
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
         * @return true if the standard is stable and no longer subject, false if the standard is experimental and not yet finalized.
         */
        public boolean isStable() {
            return stable;
        }
    }


    public enum Type { CORE, EXTENSION }

    final APIConformance parent;

    /**
     * Conformance class identifier.
     */
    final String id;

    /**
     * Indicates standard approval level.
     */
    final Level level;

    private final Type type;

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
     * @param status standard approval status
     */
    public APIConformance(String id, Level level) {
        this( id, level, Type.EXTENSION, null );
    }

    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param status standard approval status
     */
    public APIConformance(String id, Level level, Type type, APIConformance parent) {
        this.id = id;
        this.level = level;
        this.type = type;
        this.parent = parent;
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
        return "APIConformance ( " + id + " " + level + " )";
    }
}
