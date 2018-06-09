/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a CSW repository item, that is, the eventual data associated to a certain CSW Record.
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface RepositoryItem {

    /** The MIME type describing the repository item contents */
    public String getMime();

    /** The repository item contents */
    public InputStream getContents() throws IOException;
}
