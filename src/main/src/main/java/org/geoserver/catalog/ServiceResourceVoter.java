/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Allows to verify if a given layer is suitable for publishing on a given service. A single
 * negative vote will make the service disappear from the selective service layer UI
 *
 * @author Andrea Aime, Geosolutions
 * @author Fernando Mino, Geosolutions
 */
public interface ServiceResourceVoter {

    /**
     * Returns true if the services is not considered suitable for the given layer. In case the
     * answer is unknown by this voter, false will be returned
     */
    boolean hideService(String service, ResourceInfo resource);
}
