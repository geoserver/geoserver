/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Collections;
import java.util.List;
import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.util.Version;

/**
 * This interface is essentially an alias for the Generic ExtendedCapabilitiesProvider so that the
 * Type Parameters do not need to be declared everywhere and so that when loading extensions there
 * is a distinct class of beans to load
 *
 * @author Jesse Eichar, camptocamp
 */
public interface WFSExtendedCapabilitiesProvider
        extends ExtendedCapabilitiesProvider<WFSInfo, GetCapabilitiesRequest> {

    /**
     * Returns the extra profiles this plugin is adding to the base specification for the given
     * service version. Called only for version >= 2.0.
     *
     * @return A non null list of implemented profiles (eventually empty)
     */
    default List<String> getProfiles(Version version) {
        return Collections.emptyList();
    }

    /**
     * Allows extension points to examine and eventually alter top level operation metadata
     * constraints (the default implementation does nothing). Called only for version >= 2.0.
     */
    default void updateRootOperationConstraints(Version version, List<DomainType> constraints) {
        // does nothing
    }

    /**
     * Allows extension points to examine and eventually alter single operation metadata. Called
     * only for version >= 1.1.
     *
     * @param version The service version
     * @param operations The operations
     */
    default void updateOperationMetadata(Version version, List<OperationMetadata> operations) {
        // does nothing
    }
}
