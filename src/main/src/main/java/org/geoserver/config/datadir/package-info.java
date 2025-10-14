/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Provides an optimized GeoServer data directory loader implementation.
 *
 * <p>This is the default data directory loader since GeoServer 2.27, and can be disabled through the
 * {@code GEOSERVER_DATA_DIR_LOADER_ENABLED=false} system property or environment variable.
 *
 * <p>This package contains classes for efficiently loading the GeoServer catalog and configuration from the data
 * directory. The implementation is particularly optimized for large catalogs and network filesystems (like NFS) where
 * loading thousands of small XML files sequentially would be slow.
 *
 * <p>Key features of this implementation:
 *
 * <ul>
 *   <li>Parallel file reading and XML parsing
 *   <li>Single-pass directory traversal for both catalog and configuration
 *   <li>Thread-local XStream persisters to avoid contention
 *   <li>Deferred password decryption to avoid threading issues
 * </ul>
 *
 * <p>Main classes in this package:
 *
 * <ul>
 *   <li>{@link org.geoserver.config.datadir.DataDirectoryGeoServerLoader} - The main entry point that replaces the
 *       {@link org.geoserver.config.DefaultGeoServerLoader}. Coordinates the overall loading process
 * </ul>
 *
 * <p>Internal implementation details are in the following classses:
 *
 * <ul>
 *   <li>{@link org.geoserver.config.datadir.DataDirectoryWalker} - Efficiently traverses the data directory structure
 *   <li>{@link org.geoserver.config.datadir.CatalogLoader} - Loads catalog entities (workspaces, stores, layers, etc.)
 *   <li>{@link org.geoserver.config.datadir.ConfigLoader} - Loads configuration entities (services, settings, etc.)
 *   <li>{@link org.geoserver.config.datadir.XStreamLoader} - Thread-safe XML deserialization
 * </ul>
 *
 * <p>The implementation uses parallel streams and a fork-join pool to maximize throughput, especially when loading from
 * network filesystems like NFS. It tries to make a single-pass over the data directory structure and uses thread-local
 * XStream persisters to avoid contention.
 *
 * @since 2.27
 */
package org.geoserver.config.datadir;
