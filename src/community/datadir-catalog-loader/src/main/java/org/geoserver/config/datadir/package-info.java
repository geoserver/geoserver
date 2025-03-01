/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Provides an optimized GeoServer data directory loader implementation.
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
 *       default GeoServerLoader
 *   <li>{@link org.geoserver.config.datadir.config.DataDirectoryGeoServerLoaderConfiguration} - Spring configuration to
 *       register the loader
 * </ul>
 *
 * <p>Internal implementation details are in the {@link org.geoserver.config.datadir.internal} package.
 *
 * <p>This is the default data directory loader since GeoServer 2.27, and can be disabled through the
 * {@code GEOSERVER_DATA_DIR_LOADER_ENABLED=false} system property or environment variable.
 *
 * @since 2.27
 */
package org.geoserver.config.datadir;
