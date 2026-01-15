/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.metadata.citation.Citation;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.metadata.iso.IdentifierImpl;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.util.CanonicalSet;
import org.geotools.util.logging.Logging;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean responsible for creating and registering custom {@link org.geotools.api.referencing.crs.CRSAuthorityFactory}
 * instances from the GeoServer <code>$GEOSERVER_DATA_DIR/user_projections/authorities.properties</code> and <code>
 * $GEOSERVER_DATA_DIR/user_projections/<authorityPrefix>.properties</code> files.
 */
public class CustomCRSAuthorityLoader implements InitializingBean, GeoServerLifecycleHandler {
    private static final Logger LOGGER = Logging.getLogger(CustomCRSAuthorityLoader.class);
    public static final String USER_AUTHORITIES_LOCATION = "user_projections/authorities.properties";

    private final GeoServerResourceLoader resourceLoader;

    /**
     * Set of canonical citations for the custom authorities loaded, ensures that only one instance of each citation is
     * created.
     */
    private static final CanonicalSet<Citation> canonicalCitations = CanonicalSet.newInstance(Citation.class);

    public CustomCRSAuthorityLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Looks up the custom authorities,loads the property files, and programmatically registers the authorities involved
     * in GeoTools factory registry.
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        reloadCustomAuthorities();
    }

    @Override
    public void onReset() {
        reloadCustomAuthorities();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {
        // reload the custom authorities before reloading the catalog, so that any CRS used in the catalog is available
        reloadCustomAuthorities();
    }

    @Override
    public void onReload() {}

    private void reloadCustomAuthorities() {
        Resource authoritiesConfig = resourceLoader.get(USER_AUTHORITIES_LOCATION);
        if (authoritiesConfig.getType() == Resource.Type.RESOURCE) {
            LOGGER.config(String.format("%s was found, loading custom CRS authorities.", USER_AUTHORITIES_LOCATION));
            Properties authorities = new Properties();
            try (InputStream is = authoritiesConfig.in()) {
                authorities.load(is);
                List<CRSAuthorityFactory> factories = loadCustomCRSAuthorities(authorities);
                // unload and reload while CRS critical section operations are blocked
                synchronized (CRS.class) {
                    unloadCustomAuthorities();
                    loadCustomAuthorities(factories);
                }
            } catch (Exception e) {
                LOGGER.severe(String.format(
                        "Error loading custom CRS authorities configuration from %s: %s",
                        USER_AUTHORITIES_LOCATION, e.getMessage()));
            }
        } else {
            unloadCustomAuthorities();
        }
    }

    private void loadCustomAuthorities(List<CRSAuthorityFactory> factories) {
        for (CRSAuthorityFactory factory : factories) {
            ReferencingFactoryFinder.addAuthorityFactory(factory);
            LOGGER.fine(String.format("Registered custom CRS authority for %s", factory.getAuthority()));
        }
    }

    private void unloadCustomAuthorities() {
        Set<CRSAuthorityFactory> factories = new HashSet<>(ReferencingFactoryFinder.getCRSAuthorityFactories(null));
        for (CRSAuthorityFactory factory : factories) {
            if (factory instanceof UserAuthorityWKTFactory || factory instanceof UserAuthorityLongitudeFirstFactory) {
                ReferencingFactoryFinder.removeAuthorityFactory(factory);
                LOGGER.fine(String.format("Unregistered custom CRS authority for %s", factory.getAuthority()));
            }
        }
    }

    /** Data class holding information about a custom authority */
    static final class AuthorityData {
        private final String prefix;
        private final String title;
        private final Resource definitions;
        /**
         * @param prefix The authority prefix
         * @param title The authority title
         * @param definitions The resource holding the WKT definitions file
         */
        AuthorityData(String prefix, String title, Resource definitions) {
            this.prefix = prefix;
            this.title = title;
            this.definitions = definitions;
        }

        public Citation toCitation() {
            CitationImpl authority = new CitationImpl(title);
            authority.getIdentifiers().add(new IdentifierImpl(prefix));
            authority.freeze();
            return canonicalCitations.unique(authority);
        }

        boolean exists() {
            if (definitions.getType() == Resource.Type.RESOURCE) return true;
            LOGGER.warning("Properties file for custom authority '" + prefix + "' not found.");
            return false;
        }

        public String prefix() {
            return prefix;
        }

        public String title() {
            return title;
        }

        public Resource definitions() {
            return definitions;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            AuthorityData that = (AuthorityData) obj;
            return Objects.equals(this.prefix, that.prefix)
                    && Objects.equals(this.title, that.title)
                    && Objects.equals(this.definitions, that.definitions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, title, definitions);
        }

        @Override
        public String toString() {
            return "AuthorityData[" + "prefix="
                    + prefix + ", " + "title="
                    + title + ", " + "definitions="
                    + definitions + ']';
        }
    }

    /**
     * Loads custom CRS authorities from the given properties
     *
     * @param authorities The properties holding authority prefix to title mappings
     * @return A list of custom CRS authority factories
     */
    private List<CRSAuthorityFactory> loadCustomCRSAuthorities(Properties authorities) {
        return authorities.entrySet().stream()
                .map(e -> {
                    String prefix = (String) e.getKey();
                    String title = (String) e.getValue();
                    return new AuthorityData(
                            prefix, title, resourceLoader.get("user_projections/" + prefix + ".properties"));
                })
                .filter(AuthorityData::exists)
                .flatMap(CustomCRSAuthorityLoader::getFactories)
                .collect(Collectors.toList());
    }

    /**
     * Returns the factories for the given authority data, generating a unique class for each authority (check
     * AuthorityFactoryCompiler for details)
     *
     * @param ad the authority data containing prefix, title, and CRS definitions resource
     * @return a stream of CRS authority factories for the given authority
     */
    private static @NonNull Stream<CRSAuthorityFactory> getFactories(AuthorityData ad) {
        UserAuthorityWKTFactory baseFactory =
                AuthorityFactoryCompiler.buildUserAuthority(ad.prefix, ad.toCitation(), ad.definitions());
        UserAuthorityLongitudeFirstFactory longitudeFirstFactory =
                AuthorityFactoryCompiler.buildLongitudeFirstAuthority(ad.prefix, baseFactory);

        return Stream.of(longitudeFirstFactory, baseFactory);
    }
}
