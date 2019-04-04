/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

/**
 * Allows to run a transformation on the coverage or feature collection being encoded via GDAL/OGR
 * to cope with format specific limitations. It is explicitly configured in the output format
 * configuration file
 *
 * @author Andrea Aime
 * @param <T>
 */
public interface FormatAdapter<T> {

    T adapt(T input);
}
