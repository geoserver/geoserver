/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import java.util.Optional;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.TileCache;
import org.eclipse.imagen.TileScheduler;
import org.eclipse.imagen.media.util.CacheDiagnostics;
import org.geoserver.platform.ModuleStatus;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;

/** Reports ImageN library version and tile cache usage. */
public class ImageNStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "imagen";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.of("Image processing");
    }

    @Override
    public String getName() {
        return "ImageN";
    }

    @Override
    public Optional<String> getVersion() {
        Version version = GeoTools.getVersion(ImageN.class);
        return Optional.ofNullable(version).map(Version::toString);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public Optional<String> getMessage() {
        ImageN imageN = ImageN.getDefaultInstance();
        TileCache cache = imageN.getTileCache();
        TileScheduler scheduler = imageN.getTileScheduler();

        StringBuilder message = new StringBuilder();
        message.append("Tile cache: ").append(cache.getClass().getSimpleName()).append("\n");
        message.append("Memory capacity: ")
                .append(megabytes(cache.getMemoryCapacity()))
                .append("\n");
        if (cache instanceof CacheDiagnostics diagnostics) {
            message.append("Memory used: ")
                    .append(megabytes(diagnostics.getCacheMemoryUsed()))
                    .append("\n");
            message.append("Tile count: ")
                    .append(diagnostics.getCacheTileCount())
                    .append("\n");
        }
        message.append("Memory threshold: ")
                .append(Math.round(100 * cache.getMemoryThreshold()))
                .append("%\n");
        message.append("Tile threads: ").append(scheduler.getParallelism()).append("\n");
        message.append("Tile thread priority: ").append(scheduler.getPriority());
        return Optional.of(message.toString());
    }

    private static String megabytes(long bytes) {
        return "%.1f MB".formatted(bytes / (1024.0 * 1024.0));
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.of("configuration/image_processing/index.html");
    }

    @Override
    public Category getCategory() {
        return Category.CORE;
    }
}
