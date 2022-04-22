/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.libdeflate;

import it.geosolutions.imageio.compression.CompressionRegistry;
import it.geosolutions.imageio.compression.CompressorSpi;
import it.geosolutions.imageio.compression.DecompressorSpi;
import it.geosolutions.imageio.compression.libdeflate.LibDeflateCompressorSpi;
import it.geosolutions.imageio.compression.libdeflate.LibDeflateDecompressorSpi;
import java.util.Iterator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;

/** Simple initializer to populate the Libdeflate settings on first usage */
public class LibdeflateSettingsInitializer implements GeoServerInitializer {

    @Override
    public void initialize(GeoServer geoServer) {
        // Add a new Element to the metadata map
        GeoServerInfo global = geoServer.getGlobal();
        MetadataMap metadata = global.getSettings().getMetadata();
        if (!metadata.containsKey(LibdeflateSettings.LIBDEFLATE_SETTINGS_KEY)) {
            metadata.put(LibdeflateSettings.LIBDEFLATE_SETTINGS_KEY, new LibdeflateSettings());
            geoServer.save(global);
        } else {
            initSettings(
                    (LibdeflateSettings) metadata.get(LibdeflateSettings.LIBDEFLATE_SETTINGS_KEY));
        }
    }

    public static void initSettings(LibdeflateSettings libdeflateSettings) {
        if (libdeflateSettings == null) return;

        CompressionRegistry registryInstance = CompressionRegistry.getDefaultInstance();
        Iterator<CompressorSpi> cSpis = registryInstance.getSPIs(CompressorSpi.class, true);
        while (cSpis.hasNext()) {
            CompressorSpi spi = cSpis.next();
            if (spi instanceof LibDeflateCompressorSpi) {
                LibDeflateCompressorSpi compSpi = ((LibDeflateCompressorSpi) spi);
                compSpi.setPriority(libdeflateSettings.compressionPriority);
                compSpi.setMaxLevel(libdeflateSettings.maxLevel);
                compSpi.setMinLevel(libdeflateSettings.minLevel);
                compSpi.onRegistration(registryInstance, CompressorSpi.class);
                break;
            }
        }

        Iterator<DecompressorSpi> dSpis =
                CompressionRegistry.getDefaultInstance().getSPIs(DecompressorSpi.class, true);
        while (dSpis.hasNext()) {
            DecompressorSpi spi = dSpis.next();
            if (spi instanceof LibDeflateDecompressorSpi) {
                LibDeflateDecompressorSpi decompSpi = (LibDeflateDecompressorSpi) spi;
                decompSpi.setPriority(libdeflateSettings.decompressionPriority);
                decompSpi.onRegistration(registryInstance, DecompressorSpi.class);
                break;
            }
        }
    }
}
