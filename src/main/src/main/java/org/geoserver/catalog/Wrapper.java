/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Interface for classes which provide the ability to retrieve the delegate instance when the
 * instance in question is in fact a proxy/decorator class.
 *
 * <p>Developers may wish to gain access to the resources that are wrapped (the delegates) as proxy
 * class instances representing the the actual resources.
 *
 * <p>This interface aims to be compatible with the JDK 1.6 Wrapper class, which cannot be used in
 * this release of GeoServer due to requirements of being JDK 1.5 compatible.
 */
public interface Wrapper {

    /**
     * Returns an object that implements the given interface to allow access to non-standard
     * methods, or standard methods not exposed by the proxy.
     *
     * <p>If the receiver implements the interface then the result is the receiver or a proxy for
     * the receiver. If the receiver is a wrapper and the wrapped object implements the interface
     * then the result is the wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling <code>unwrap</code> recursively on the wrapped object or a proxy for
     * that result. If the receiver is not a wrapper and does not implement the interface, then an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing
     *     object.
     * @throws java.sql.IllegalArgumentException If no object found that implements the interface
     */
    <T> T unwrap(java.lang.Class<T> iface) throws IllegalArgumentException;

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a
     * wrapper for an object that does. Returns false otherwise. If this implements the interface
     * then return true, else if this is a wrapper then return the result of recursively calling
     * <code>isWrapperFor</code> on the wrapped object. If this does not implement the interface and
     * is not a wrapper, return false. This method should be implemented as a low-cost operation
     * compared to <code>unwrap</code> so that callers can use this method to avoid expensive <code>
     * unwrap</code> calls that may fail. If this method returns true then calling <code>unwrap
     * </code> with the same argument should succeed.
     *
     * @param iface a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that
     *     does.
     */
    boolean isWrapperFor(java.lang.Class<?> iface);
}
