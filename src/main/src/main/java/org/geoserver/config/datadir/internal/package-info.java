/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Internal implementation classes for the optimized GeoServer data directory loader.
 *
 * <p>This package contains the core classes that implement the efficient, parallel loading of GeoServer's catalog and
 * configuration from the data directory. These classes are not meant to be used directly by client code, but are
 * instead used by the {@link org.geoserver.config.datadir.DataDirectoryGeoServerLoader}.
 *
 * <p>Key components in this package:
 *
 * <ul>
 *   <li>{@link org.geoserver.config.datadir.internal.DataDirectoryLoader} - Package-level interface. Coordinates the
 *       overall loading process
 *   <li>{@link org.geoserver.config.datadir.internal.DataDirectoryWalker} - Efficiently traverses the data directory
 *       structure
 *   <li>{@link org.geoserver.config.datadir.internal.CatalogLoader} - Loads catalog entities (workspaces, stores,
 *       layers, etc.)
 *   <li>{@link org.geoserver.config.datadir.internal.ConfigLoader} - Loads configuration entities (services, settings,
 *       etc.)
 *   <li>{@link org.geoserver.config.datadir.internal.XStreamLoader} - Thread-safe XML deserialization
 * </ul>
 *
 * <p>The implementation uses parallel streams and a fork-join pool to maximize throughput, especially when loading from
 * network filesystems like NFS. It tries to make a single-pass over the data directory structure and uses thread-local
 * XStream persisters to avoid contention.
 *
 * @since 2.27
 */
package org.geoserver.config.datadir.internal;
