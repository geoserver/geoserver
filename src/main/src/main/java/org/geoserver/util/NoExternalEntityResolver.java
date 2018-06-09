/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

/**
 * EntityResolver implementation to prevent usage of external entity references. to access local
 * files. When parsing an XML entity, the empty InputSource returned by this resolver provokes
 * throwing of a java.net.MalformedURLException, which can be handled appropriately.
 *
 * @author Davide Savazzi - geo-solutions.it
 * @deprecated Use {@link org.geotools.xml.PreventLocalEntityResolver} instead
 */
public class NoExternalEntityResolver extends org.geotools.xml.PreventLocalEntityResolver {
    private static final long serialVersionUID = -8737951974063414228L;
}
