/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.client.xml;



import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.xsd.XSDSchema;
import org.geotools.gml3.ApplicationSchemaXSD;
import org.geotools.wfs.v1_1.WFS;
import org.geotools.xml.SchemaLocationResolver;

/**
 * Improved schema resolution schema behavior over GeoTools' implementation.
 * Support for #230.
 *
 * @author dparker
 */
public class ResolvingApplicationSchemaXSD extends ApplicationSchemaXSD {
    private static final Log LOG = LogFactory.getLog(ResolvingApplicationSchemaXSD.class);

    /**
     * Invokes {@link ApplicationSchemaXSD#ApplicationSchemaXSD(String, String)
     * supertype constructor}, q.v.
     */
    public ResolvingApplicationSchemaXSD(String namespaceURI, String schemaLocation) {
        super(namespaceURI, schemaLocation);
    }
    
    @Override
    protected void addDependencies(Set dependencies) {
        super.addDependencies(dependencies);
        dependencies.add(WFS.getInstance());
    }

    /**
     * Uses the <code>schema.getSchemaLocation()</code>'s parent folder as the
     * base folder to resolve <code>location</code> as a relative URI of.
     * <p>
     * This way, application schemas splitted over multiple files can be
     * resolved based on the relative location of a given import or include.
     * </p>
     *
     * @param schema
     *            the schema being resolved
     * @param uri
     *            not used as it might be an empty string when location refers
     *            to an include
     * @param location
     *            the xsd location, either of <code>schema</code>, an import or
     *            an include, for which to try resolving it as a relative path
     *            of the <code>schema</code> location.
     * @return a file: style uri with the resolved schema location for the given
     *         one, or <code>null</code> if <code>location</code> can't be
     *         resolved as a relative path of the <code>schema</code> location.
     */
    @Override
    public SchemaLocationResolver createSchemaLocationResolver() {
        return new SchemaLocationResolver(this) {
            @Override
            public String resolveSchemaLocation(XSDSchema schema, String uri, String location) {
                if(location == null) {
                    return null;
                }
                String schemaLocation;

                if(schema == null) {
                    schemaLocation = getSchemaLocation();
                    return null;
                } else {
                    schemaLocation = schema.getSchemaLocation();
                }

                if(schemaLocation == null) {
                    try {
                        return new URL(location).toString();
                    } catch(MalformedURLException e) {
                        LOG.warn("Can't resolve " + location + " to an absolute location.");
                        return null;
                    }
                } else {
                    try {
                        return new URL(new URL(schemaLocation), location).toExternalForm();
                    } catch(MalformedURLException e) {
                        LOG.warn("Can't resolve " + location + " against " + schemaLocation, e);
                        return null;
                    }
                }
            }
        };
    }

}
