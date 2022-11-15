/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Allows to verify if a given layer is suitable for publishing for a given service type.
 *
 * <p>A single negative vote will make the service disappear from the selective service layer UI and
 * welcome page.
 *
 * @author Andrea Aime, Geosolutions
 * @author Fernando Mino, Geosolutions
 */
public interface ServiceResourceVoter {

    /**
     * Returns {@code true} if the services is not considered suitable for the given layer. In case
     * the answer is unknown by this voter, false will be returned
     *
     * @param serviceType Service type
     * @param resource
     * @return true if voter knows the resource is not suitable for use with service type
     */
    boolean hideService(String serviceType, ResourceInfo resource);
}
