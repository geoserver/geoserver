/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.data.ows.URLChecker;
import org.geotools.data.ows.URLCheckers;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

/**
 * GeoServer implementation of the {@link URLChecker} interface, based on a list of {@link
 * RegexURLCheck} provided by the {@link URLCheckDAO}
 */
public class GeoServerURLChecker implements URLChecker, DisposableBean {

    static final Logger LOGGER = Logging.getLogger(GeoServerURLChecker.class);

    private final URLCheckDAO dao;

    public GeoServerURLChecker(URLCheckDAO dao) {
        this.dao = dao;
        URLCheckers.register(this);
    }

    @Override
    public String getName() {
        return "Geoserver";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean confirm(String url) {
        try {
            // enabled? if not then allow everything
            if (!dao.isEnabled()) return true;

            // null safety
            if (url == null || url.isEmpty()) return false;

            // if enabled but no checks configured, deny access
            List<AbstractURLCheck> enabledUrlList = getEnabledChecks();
            if (!enabledUrlList.isEmpty()) {
                for (AbstractURLCheck u : enabledUrlList) {
                    if (u.confirm(url)) {
                        LOGGER.log(Level.FINE, () -> "URL " + url + " was matched by " + u);
                        return true;
                    }
                }
            }
            LOGGER.log(Level.FINE, () -> url + " did not match any check");

            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error while checking URL " + url, e);
        }
    }

    private List<AbstractURLCheck> getEnabledChecks() throws Exception {
        return dao.getChecks().stream().filter(e -> e.isEnabled()).collect(Collectors.toList());
    }

    public AbstractURLCheck get(final String name) throws Exception {
        Optional<AbstractURLCheck> entry =
                dao.getChecks().stream()
                        .filter(urlEntry -> urlEntry.getName().equalsIgnoreCase(name))
                        .findFirst();
        if (entry.isPresent()) {
            return entry.get();
        } else {
            return null;
        }
    }

    @Override
    public void destroy() throws Exception {
        // necessary, otherwise the SPI will hold a reference to the bean across restarts/tests
        URLCheckers.deregister(this);
    }
}
