/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MetatileContextHolder;

/**
 * Marker interface for {@link GetMapOutputFormat} implementations that support meta-tiling.
 *
 * <p>A {@code MetaTilingOutputFormat} is able to serve a single request covering a meta-tile (a grid of multiple tiles)
 * and internally split the result into the individual sub-tiles.
 *
 * <p>During a meta-tiled request, information about the current meta-tile (such as tile size and meta-tile dimensions)
 * is provided via {@link MetatileContextHolder}. Implementations will check the meta-tile context to compute
 * per-subtile bounds, clipping, and coordinate transforms. When no meta-tile context is present, the output format
 * should behave as a normal single-tile {@code GetMapOutputFormat}.
 *
 * @see MetatileContextHolder
 * @see GetMapOutputFormat
 */
public interface MetaTilingOutputFormat extends GetMapOutputFormat {}
