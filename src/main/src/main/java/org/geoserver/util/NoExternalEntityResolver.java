/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

/**
 * EntityResolver implementation to prevent usage of external entities.
 * 
 * When parsing an XML entity, the empty InputSource returned by this resolver provokes 
 * throwing of a java.net.MalformedURLException, which can be handled appropriately.
 * 
 * @author Davide Savazzi - geo-solutions.it
 * @deprecated Use {@link org.geotools.xml.NoExternalEntityResolver} instead
 */
public class NoExternalEntityResolver extends org.geotools.xml.NoExternalEntityResolver {

}