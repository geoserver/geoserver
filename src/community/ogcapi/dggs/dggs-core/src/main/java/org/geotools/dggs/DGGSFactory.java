/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.dggs;

import java.io.IOException;
import java.util.Map;
import org.geotools.data.Parameter;

/** Factory creating DGGS intances */
public interface DGGSFactory {

    /**
     * Identifier of the factory
     *
     * @return
     */
    public String getId();

    /**
     * MetaData about the required Parameters (for #createDGGSInstance).
     *
     * @return Param array describing the Map for createDataStore
     */
    Parameter[] getParametersInfo();

    /**
     * Creates an instance of
     *
     * @param params
     * @return
     */
    DGGSInstance createInstance(Map<String, ?> params) throws IOException;

    /**
     * Test to see if the implementation is available for use. This method ensures all the
     * appropriate libraries to construct the {@link DGGSInstance} are available.
     */
    boolean isAvailable();
}
