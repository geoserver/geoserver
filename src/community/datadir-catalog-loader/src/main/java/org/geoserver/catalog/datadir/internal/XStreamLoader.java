/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir.internal;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

class XStreamLoader {

    private static final Logger LOGGER =
            Logging.getLogger(XStreamLoader.class.getPackage().getName());

    private static final XStreamPersisterFactory xpf = new XStreamPersisterFactory();

    private static final ThreadLocal<XStreamPersister> XP =
            ThreadLocal.withInitial(xpf::createXMLPersister);

    private final AtomicLong readFileCount = new AtomicLong();

    @SuppressWarnings("unchecked")
    public <C extends Info> Optional<C> depersist(Path file, Catalog catalog) {
        C info = null;
        try (InputStream in = Files.newInputStream(file, StandardOpenOption.READ)) {
            XStreamPersister xp = getXstream(catalog);
            Info depersisted = xp.load(in, Info.class);
            if (null == depersisted) {
                LOGGER.log(Level.WARNING, file + " depersisted to null");
            } else {
                readFileCount.incrementAndGet();
                info = (C) depersisted;
            }
        } catch (Exception e) {
            String err = e.getMessage();
            if (!StringUtils.hasText(err)) err = e.getCause().getMessage();
            LOGGER.log(Level.WARNING, "Error depersisting " + file + ": " + err);
            info = null;
        }
        return Optional.ofNullable(info);
    }

    private XStreamPersister getXstream(Catalog catalog) {
        XStreamPersister xp = XP.get();
        xp.setCatalog(catalog);
        xp.setUnwrapNulls(false);
        // disable password decrypt at this stage, or xp will use GeoServerExtensions to
        // lookup the
        // GeoServerSecurityManager, dead-locking on the main thread
        xp.setEncryptPasswordFields(false);
        return xp;
    }
}
