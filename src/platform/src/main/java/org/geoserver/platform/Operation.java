/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An operation descriptor providing metadata about a service operation.
 * <p>
 * An operation is identified by an id,service pair. Two operation
 * descriptors are considred equal if they have the same id, service pair.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public final class Operation {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(Operation.class);
    /**
     * Unique identifier withing service of the operation.
     */
    private final String id;

    /**
     * Service this operation is a component of.
     */
    private final Service service;

    /**
     * The method implementing the operation
     */
    private final Method method;

    /**
     * Parameters of the operation
     */
    private final Object[] parameters;

    /**
     * Creates a new operation descriptor.
     *
     * @param id Id of the operation, must not be <code>null</code>
     * @param service The service containing the operation, must not be <code>null</code>
     * @param method THe method implementing the operation.
     * @param parameters The parameters of the operation, may be <code>null</code>
     *
     */
    public Operation(final String id, final Service service, final Method method, final Object[] parameters) {
        this.id = id;
        this.service = service;
        this.method = method;
        this.parameters = parameters;

        if (id == null) {
            LOGGER.log(Level.SEVERE, "NullPointerException {0}", "id");
            throw new NullPointerException("id");
        }

        if (service == null) {
            LOGGER.log(Level.SEVERE, "NullPointerException {0}", "service");
            throw new NullPointerException("service");
        }
        if (method == null) {
            LOGGER.log(Level.SEVERE, "NullPointerException {0}", "method");
            throw new NullPointerException("method");
        }
        if (parameters == null) {
            LOGGER.log(Level.SEVERE, "NullPointerException {0}", "parameters");
            throw new NullPointerException("parameters");
        }
    }

    /**
     * @return The id of the operation.
     */
    public final String getId() {
        return id;
    }

    /**
     * @return The service implementing the operation.
     */
    public final Service getService() {
        return service;
    }

    /**
     * @return The method implementing the operation.
     */
    public final Method getMethod() {
        return method;
    }

    /**
     * @return The parameters supplied to the operation
     */
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Operation)) {
            return false;
        }

        final Operation other = (Operation) obj;

        if (!id.equals(other.id)) {
            return false;
        }

        return service.equals(other.service);
    }

    @Override
    public final int hashCode() {
        return (id.hashCode() * 17) + service.hashCode();
    }

    @Override
    public final String toString() {
        return "Operation( " + id + ", " + service.getId() + " )";
    }
}
