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
package org.geoserver.web.data.layergroup;

import java.io.Serializable;
import org.geoserver.catalog.LayerGroupInfo;

/**
 * Interface for a layer group filter. It extends Serializable so it can be serialized in a Wicket
 * session.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public interface LayerGroupProviderFilter extends Serializable {

    boolean accept(LayerGroupInfo group);
}
