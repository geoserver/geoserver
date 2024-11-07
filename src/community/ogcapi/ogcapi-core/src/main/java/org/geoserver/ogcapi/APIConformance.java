package org.geoserver.ogcapi;

import java.util.Objects;

/**
 * Service capability or feature, identified by conformance class.
 *
 * OGCAPI Web Services are defined with core functionality, strictly extended with additional, optional, functionality
 * identified by "conformance class".
 *
 * OGC Open Web Services can be extended using application profiles with additional, optional, functionality.
 */
public class APIConformance {

    /**
     * Standards approval status.
     */
    public enum Status {
        APPROVED, DRAFT, INFORMAL
    }

    public enum Type { CORE, EXTENSION }

    final APIConformance parent;

    /**
     * Conformance class identifier.
     */
    final String id;

    /**
     * Indicates standard approval status.
     */
    final Status status;

    private final Type type;

    /**
     * Conformance class declaration, defaulting to APPROVED.
     *
     * @param id conformance class
     */
    public APIConformance(String id) {
        this(id, Status.APPROVED);
    }

    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param status standard approval status
     */
    public APIConformance(String id, Status status) {
        this( id, status, Type.EXTENSION, null );
    }

    /**
     * Conformance class declaration.
     *
     * @param id conformance class
     * @param status standard approval status
     */
    public APIConformance(String id, Status status, Type type, APIConformance parent) {
        this.id = id;
        this.status = status;
        this.type = type;
        this.parent = parent;
    }

    public APIConformance extend(String id) {
        return new APIConformance(id, Status.APPROVED, Type.EXTENSION, this);
    }

    public APIConformance extend(String id, Status status) {
        return new APIConformance(id, status, Type.EXTENSION, this);
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
     * Conformance class standard approval status.
     *
     * @return conformance class standard approval status.
     */
    public Status getStatus() {
        return status;
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
        return "APIConformance ( " + id + " " + status + " )";
    }
}
