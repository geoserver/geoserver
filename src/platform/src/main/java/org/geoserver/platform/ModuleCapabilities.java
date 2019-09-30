package org.geoserver.platform;

public interface ModuleCapabilities {

    enum Capability {
        ADVANCED_SECURITY_CONFIG
    }

    default boolean hasCapability(Capability capability) {
        return false;
    }
}
