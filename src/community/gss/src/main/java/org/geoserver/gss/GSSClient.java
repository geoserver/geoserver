/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.io.IOException;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionType;

/**
 * Allows communication with a specific GSS Unit service
 * 
 * @author Andrea Aime
 * 
 */
public interface GSSClient {

    /**
     * Grabs the latest central revision number known to the client for the specified layer
     */
    public long getCentralRevision(QName layerName) throws IOException;

    /**
     * Posts the changes occurred locally between fromRevision and toRevision to the client
     */
    public void postDiff(PostDiffType postdiff)
            throws IOException;

    /**
     * Grabs the changes occurred on the unit since the fromVersion unit revision
     */
    public GetDiffResponseType getDiff(GetDiffType getDiff) throws IOException;
}
