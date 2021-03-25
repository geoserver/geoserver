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

import org.geotools.data.CloseableIterator;
import org.geotools.feature.visitor.CalcResult;

/**
 * A CalcResult that can return a {@link CloseableIterator}, providing a streaming option to get
 * large result sets
 *
 * @param <T>
 */
public interface IterableCalcResult<T> extends CalcResult {

    public CloseableIterator<T> getIterator();
}
