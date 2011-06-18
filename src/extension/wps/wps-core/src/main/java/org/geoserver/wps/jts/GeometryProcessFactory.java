/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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
 *    
 */
package org.geoserver.wps.jts;

import org.geotools.util.SimpleInternationalString;

/**
 * A process factory exposing all the annotated methods in {@link GeometryFunctions}
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class GeometryProcessFactory extends StaticMethodsProcessFactory<GeometryFunctions> {

    public GeometryProcessFactory() {
        super(new SimpleInternationalString("Simple JTS based spatial analysis methods"), "JTS",
                GeometryFunctions.class);
    }

}
