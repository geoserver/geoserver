/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.catalog;

import java.util.List;

/**
 * 
 * 
 * @author Davide Savazzi - GeoSolutions
 */
public interface PublishedInfo extends CatalogInfo {

    /**
     * Returns the name.
     */
    String getName();
    
    /**
     * Sets the name.
     */    
    void setName(String name);

    /**
     * The derived prefixed name.
     * <p>
     * If a workspace is set this method returns:
     * <pre>
     *   getWorkspace().getName() + ":" + getName();
     * </pre>
     * Otherwise it simply returns: <pre>getName()</pre>
     * </p>
     */
    String prefixedName();

    /**
     * Returns the title.
     */
    String getTitle();
    
    /**
     * Sets the title.
     */
    void setTitle(String title);

    /**
     * Returns the abstract.
     */
    String getAbstract();
    
    /**
     * Sets the abstract.
     */
    void setAbstract(String abstractTxt);

    /**
     * A persistent map of metadata.
     */
    MetadataMap getMetadata();

    /**
     * Returns the list of authority URLs
     */
    List<AuthorityURLInfo> getAuthorityURLs();

    /**
     * Returns the list of identifiers
     */
    List<LayerIdentifierInfo> getIdentifiers();
    
}