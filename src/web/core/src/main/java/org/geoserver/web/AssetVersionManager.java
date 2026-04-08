/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import jakarta.servlet.ServletContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ManifestLoader;
import org.geoserver.ManifestLoader.AboutModel;
import org.geoserver.ManifestLoader.AboutModel.ManifestModel;
import org.geotools.util.logging.Logging;

/**
 * Computes and caches deployment/build-based version tokens for static web assets.
 *
 * <p>A single token is derived from GeoServer manifest metadata (prefer {@code Build-Timestamp}, fallback to
 * {@code Git-Revision}, then {@code Version}) and reused for all assets until cache clear/redeploy.
 *
 * <p>Usage:
 *
 * <pre>
 *   String url = AssetVersionManager.versioned("css/geoserver.css", servletContext);
 *   // returns e.g. "css/geoserver.css?v=20260326"
 * </pre>
 */
public class AssetVersionManager {

    private static final Logger LOGGER = Logging.getLogger(AssetVersionManager.class);

    /** Optional system-property override for deployment tooling. */
    public static final String ASSETS_VERSION_PROPERTY = "geoserver.assets.version";

    private static final String DEFAULT_VERSION = "0";
    private static final int MAX_CACHE_ENTRIES = 100;

    /** Per-file cache: normalized asset path -> versioned URL (e.g. "css/geoserver.css?v=20260326"). */
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    /** Shared build/deploy token for all assets, lazily resolved. */
    private static volatile String deploymentVersion;

    private AssetVersionManager() {}

    /**
     * Returns a versioned URL for the given asset path, computing and caching on first call. Subsequent calls return
     * from cache with no I/O.
     *
     * <p>The path is normalized to strip any leading slash before use as a cache key and in the rendered URL, so
     * callers may pass either {@code "css/geoserver.css"} or {@code "/css/geoserver.css"} and get the same result.
     *
     * @param path servlet-context-relative path, e.g. {@code "css/geoserver.css"} or {@code "/css/geoserver.css"}
     * @param context servlet context (currently unused, kept for API compatibility)
     * @return normalized path with a {@code ?v=<token>} suffix
     */
    public static String versioned(String path, ServletContext context) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String version = getDeploymentVersion();
        // Safety cap: clear if cache grows beyond expected size (e.g. misuse with dynamic paths).
        // ConcurrentHashMap is thread-safe; a racy double-clear is harmless.
        if (CACHE.size() >= MAX_CACHE_ENTRIES && !CACHE.containsKey(normalizedPath)) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(normalizedPath, p -> p + "?v=" + version);
    }

    /**
     * Clears the version cache. Should be called if assets are reloaded at runtime (e.g. during development or after a
     * hot-redeploy).
     *
     * @see GeoServerApplication#clearWicketCaches()
     */
    public static void clearCache() {
        CACHE.clear();
        deploymentVersion = null;
    }

    static String getDeploymentVersion() {
        String version = deploymentVersion;
        if (version != null) return version;

        synchronized (AssetVersionManager.class) {
            version = deploymentVersion;
            if (version == null) {
                deploymentVersion = version = computeDeploymentVersion();
            }
        }
        return version;
    }

    private static String computeDeploymentVersion() {
        String override = System.getProperty(ASSETS_VERSION_PROPERTY);
        if (override != null) {
            String sanitized = sanitize(override);
            if (!DEFAULT_VERSION.equals(sanitized)) return sanitized;
        }

        try {
            AboutModel versions = ManifestLoader.getVersions();
            if (versions == null || versions.getManifests() == null) return DEFAULT_VERSION;

            String buildTimestamp = null;
            String gitRevision = null;
            String projectVersion = null;

            for (ManifestModel manifest : versions.getManifests()) {
                for (Map.Entry<String, String> entry : manifest.getEntries().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value == null || value.isBlank()) continue;

                    switch (key) {
                        case "Build-Timestamp":
                            if (buildTimestamp == null) buildTimestamp = value;
                            break;
                        case "Git-Revision":
                            if (gitRevision == null) gitRevision = value;
                            break;
                        case "Version":
                            if (projectVersion == null) projectVersion = value;
                            break;
                        default:
                            // ignore other attributes
                    }
                }
            }

            String chosen =
                    buildTimestamp != null ? buildTimestamp : gitRevision != null ? gitRevision : projectVersion;
            if (chosen == null || chosen.isBlank()) return DEFAULT_VERSION;
            return sanitize(chosen);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Unable to compute deployment asset version", t);
            return DEFAULT_VERSION;
        }
    }

    private static String sanitize(String input) {
        if (input == null) return DEFAULT_VERSION;
        String sanitized = input.trim().replaceAll("[^A-Za-z0-9._-]", "");
        if (sanitized.isEmpty()) return DEFAULT_VERSION;
        return sanitized.length() > 32 ? sanitized.substring(0, 32) : sanitized;
    }
}
