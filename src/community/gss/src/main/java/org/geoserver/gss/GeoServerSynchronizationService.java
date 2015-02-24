/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

/**
 * The synchronisation service, represents the calls that can be made to a GSS Unit (remote node)
 * 
 * @author aaime
 */
public interface GeoServerSynchronizationService {

    /**
     * Grabs the last central revision known to this Unit
     * 
     * @param request
     * @return
     */
    public CentralRevisionsType getCentralRevision(GetCentralRevisionType request);

    /**
     * Applies a diff coming from Central
     * 
     * @param request
     * @return
     */
    public PostDiffResponseType postDiff(PostDiffType request);

    /**
     * Grabs the local diffs from a certain revision, up to the last synchronisation with Central,
     * skipping over changes coming from Central itself and over conflicting features
     * 
     * @param request
     * @return
     */
    public GetDiffResponseType getDiff(GetDiffType request);
}
