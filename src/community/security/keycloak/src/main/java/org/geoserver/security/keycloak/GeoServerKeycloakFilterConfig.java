/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geotools.util.logging.Logging;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * Configuration for Keycloak authentication, wrapped for use with GeoServer. This is essentially
 * the base {@link AdapterConfig} with some additional bits to help xstream read/write XML. The
 * adapter config should be input exactly as provided by the Keycloak server.
 */
public class GeoServerKeycloakFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig, SecurityAuthProviderConfig, Cloneable {

    private static final Logger LOG = Logging.getLogger(GeoServerKeycloakFilterConfig.class);

    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String className;
    protected String userGroupServiceName;

    // this is the only relevant nugget of information stored here
    protected String adapterConfig;

    /**
     * Convert the adapter configuration into an object we can use to configure the rest of the
     * context.
     *
     * @return configuration for the Keycloak-Java adapter
     * @throws IOException if the provided string does not represent valid config
     */
    public AdapterConfig readAdapterConfig() throws IOException {
        LOG.log(Level.FINER, "GeoServerKeycloakFilterConfig.readAdapterConfig ENTRY");
        try {
            return KeycloakDeploymentBuilder.loadAdapterConfig(
                    IOUtils.toInputStream(getAdapterConfig()));
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    /**
     * Save an adapter-configuration object as a string.
     *
     * @param config config to save
     * @throws IOException if the provided config cannot be saved as a string
     */
    public void writeAdapterConfig(AdapterConfig config) throws IOException {
        LOG.log(Level.FINER, "GeoServerKeycloakFilterConfig.writeAdapterConfig ENTRY");
        ObjectMapper om = new ObjectMapper();
        setAdapterConfig(om.writeValueAsString(config));
    }

    public String getAdapterConfig() {
        return adapterConfig;
    }

    public void setAdapterConfig(String adapterConfig) {
        this.adapterConfig = adapterConfig;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    // NOTE: This implementation does a soft-copy only, and is generally pretty garbage. It isn't
    // clear if a real deep-copy is needed, or what (if anything) relies on this cloning-capability,
    // so the (rather significant) effort of manually setting all the properties has been skipped.
    // Don't be surprised if the copies behave badly though.
    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {
        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);
        GeoServerKeycloakFilterConfig target;
        try {
            target = (GeoServerKeycloakFilterConfig) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }

        if (target != null
                && allowEnvParametrization
                && gsEnvironment != null
                && GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
            target.setName((String) gsEnvironment.resolveValue(name));
        }

        return target;
    }

    @Override
    public void initBeforeSave() {
        // no-op
    }
}
