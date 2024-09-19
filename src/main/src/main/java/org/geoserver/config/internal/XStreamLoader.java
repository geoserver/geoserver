/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.internal;

import com.thoughtworks.xstream.XStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

/**
 * Utility class to aid {@link CatalogConfigLoader} and {@link GeoServerConfigLoader}
 * in depersisting {@link CatalogInfo}s from Xstream-encoded XML files.
 * <p>
 * The {@link #depersist(Path, Catalog)} method uses a per-thread {@link XStreamPersister},
 * using a {@link ThreadLocal}, since it's always called from within a {@link ForkJoinPool} thread,
 * to avoid the thread contention produced otherwise produced in {@link XStream}.
 * <p>
 * {@link XStreamPersister#setEncryptPasswordFields XStreamPersister#setEncryptPasswordFields(false)}
 * is set on all xstream persisters, and the catalog loader shall decrypt the {@link StoreInfo}
 * password fields on the {@literal main) thread after all stores are loaded into the Catalog. Otherwise
 * it'll try to obtain the GeoServerSecurityManager through GeoServerExtensions,
 * and a deadlock is produced in Spring internal code.
 *
 * @since 2.26
 */
class XStreamLoader {

    private static final Logger LOGGER =
            Logging.getLogger(XStreamLoader.class.getPackage().getName());

    static XStreamPersisterFactory xpf = new XStreamPersisterFactory();

    /**
     * Holds per-thread {@link XStreamPersister}s. No need to call {@link ThreadLocal#remove()}
     * because the calling thread dies with the managed {@link ForkJoinPool} used to call {@link
     * #depersist}.
     */
    private static final ThreadLocal<XStreamPersister> XP =
            ThreadLocal.withInitial(XStreamLoader::createXMLPersister);

    private XStreamLoader() {
        // private constructor, utility class
    }

    @SuppressWarnings("unchecked")
    public static <C extends Info> Optional<C> depersist(Path file, Catalog catalog) {
        C info = null;
        try (InputStream in = Files.newInputStream(file, StandardOpenOption.READ)) {
            XStreamPersister xp = getXStream(catalog);
            Info depersisted = xp.load(in, Info.class);
            if (null == depersisted) {
                LOGGER.log(Level.WARNING, () -> file + " depersisted to null");
            } else {
                info = (C) depersisted;
            }
        } catch (Exception e) {
            String err =
                    StringUtils.hasText(e.getMessage())
                            ? e.getMessage()
                            : e.getCause().getMessage();
            LOGGER.log(Level.WARNING, () -> "Error depersisting " + file + ": " + err);
            info = null;
        }
        return Optional.ofNullable(info);
    }

    private static XStreamPersister getXStream(Catalog catalog) {
        XStreamPersister xp = XP.get();
        xp.setCatalog(catalog);
        xp.setUnwrapNulls(false);
        // disable password decrypt at this stage, or xp will use GeoServerExtensions to
        // lookup the GeoServerSecurityManager, dead-locking on the main thread
        xp.setEncryptPasswordFields(false);
        return xp;
    }

    public static void setXpf(XStreamPersisterFactory xpf) {
        XStreamLoader.xpf = xpf;
    }

    private static XStreamPersister createXMLPersister() {
        XStreamPersisterFactory factory = xpf;
        return factory.createXMLPersister();
    }
}
