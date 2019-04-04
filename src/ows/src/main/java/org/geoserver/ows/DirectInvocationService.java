/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.lang.reflect.Method;
import org.geoserver.platform.Service;

/**
 * Hook to indicate the {@link Dispatcher} that a given service object doesn't want its operations
 * to be called through reflection to avoid the performance penalty given by the synchronization
 * inside {@link Method#invoke(Object, Object...)}.
 *
 * <p>Service beans (as targeted by {@link Service#getService()}) doesn't need to implement this
 * inteface. It actually only makes sense if the service operations execute really fast and are
 * supposed to be hit by a large number of concurrent requests, as in the case of GeoWebCache tile
 * requests, where the synchronized blocks in the reflective method invocations may impose a
 * noticeable performance degradation.
 *
 * <p>At the time of writing (March 2012) this behaviour has been noticed in both Sun Java6 and
 * Java7 JDK's, other virtual machine implementations may or may not incurr in such performance
 * penalty.
 */
public interface DirectInvocationService {

    /**
     * Provides a more direct way of invoking a service operation than using reflection.
     *
     * @param operationName the name of the operation to execute, as declared in the {@link
     *     Service#getOperations() service operations} for the service descriptor that targets this
     *     service object.
     * @param parameters the list of parameters for the actual operation, as if the actual method
     *     were invoked through {@link Method#invoke(Object, Object...)}
     * @return the operation result
     * @throws IllegalArgumentException if either the operation name or arguments list doesn't match
     *     one of the service provided operations under any other circumstances, specific to the
     *     operation being executed
     */
    Object invokeDirect(String operationName, Object[] parameters)
            throws IllegalArgumentException, Exception;
}
