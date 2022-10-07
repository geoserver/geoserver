/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest.xml;

/**
 * Represents a single BatchOperation. A batch operation can either use rules or adminrules services
 * and can be of one of these types: insert, update, delete.
 */
public class BatchOperation {

    /** Enum of available service names. */
    public enum ServiceName {
        rules,
        adminrules
    }

    /** Enum of available op Type names. */
    public enum TypeName {
        insert,
        update,
        delete,
    }

    private ServiceName service;

    private TypeName type;
    private Long id;

    private AbstractPayload payload;

    public BatchOperation() {}

    /** @return the id of the op. */
    public Long getId() {
        return id;
    }

    /**
     * Set the id of the operation.
     *
     * @param id the id to set.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return the service name of the operation. */
    public ServiceName getService() {
        return service;
    }

    /**
     * Sets the service name of the operation.
     *
     * @param service the service name to set.
     */
    public void setService(ServiceName service) {
        this.service = service;
    }

    /** @return the type name of the operation. */
    public TypeName getType() {
        return type;
    }

    /**
     * Sets the type name.
     *
     * @param type the type name to set.
     */
    public void setType(TypeName type) {
        this.type = type;
    }

    /** @return the payload of the operation. Can be {@link JaxbRule} or a {@link JaxbAdminRule}. */
    public AbstractPayload getPayload() {
        return payload;
    }

    /**
     * Sets the payload of the operation.
     *
     * @param payload the payload to set. Can be a {@link JaxbRule} or a {@link JaxbAdminRule}
     */
    public void setPayload(AbstractPayload payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "["
                + service
                + "."
                + type
                + (id != null ? " id:" + id : "")
                + (payload != null ? " payload is a " + payload.getClass().getSimpleName() : "")
                + "]";
    }
}
