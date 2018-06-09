/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/*
 * Package {@code org.geogig.geoserver.gwc} integrates GeoServer configured geogig data stores with GeoWebCache.
 * <p>
 * <H2>Truncate GWC tiles<H2>
 * <p>
 * The {@link org.geogig.geoserver.gwc.TruncateTilesOnUpdateRefHook} command hook is declared
 * in {@code src/main/resources/META-INF/services/org.geogig.api.hooks.CommandHook}, following GeoGIG's
 * SPI mechanism to declare "classpath" command hooks.
 * <p>
 * This command hook captures calls to {@link org.locationtech.geogig.api.plumbing.UpdateRef} commands in any
 * configured {@link org.locationtech.geogig.geotools.data.GeoGigDataStore geogig datastore}, and figures out
 * which {@link org.geoserver.gwc.layer.GeoServerTileLayer tile layers} would be affected by the change.
 * <p>
 * For the affected tile layers, the {@link org.geogig.geoserver.gwc.MinimalDiffBounds} command is used to compute
 * the so called "minimal bounds" of the diff between the old and new trees pointed by the ref update and
 * the layer's tree path, which is a geometry that's big enough to cover the changes but much smaller than
 * the whole bounds, so that the number of tiles truncated is minimized.
 * <p>
 * That geometry is then used as a mask to issue GWC truncate tasks for each of the layer's configured gridset and styles.
 */

package org.geogig.geoserver.gwc;
