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
 *
 * @deprecated use org.geotools.decorate.Wrapper
 */
public interface Wrapper extends org.geotools.util.decorate.Wrapper {}
