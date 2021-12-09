/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.Serializable;

/** The importer configuration */
public interface ImporterInfo extends Serializable {

    /** Directory where uploads should be stored temporarily */
    public String getUploadRoot();

    /** Maximum number of synchronous imports, negative or zero for no limit */
    public int getMaxSynchronousImports();

    /** Maximum number of synchronous imports, negative or zero for no limit */
    public int getMaxAsynchronousImports();

    /** @see #getUploadRoot() */
    public void setUploadRoot(String uploadRoot);

    /** @see #getMaxSynchronousImports() () */
    public void setMaxSynchronousImports(int maxSynchronousImports);

    /** @see #getMaxAsynchronousImports() */
    public void setMaxAsynchronousImports(int maxAsynchronousImports);

    /**
     * Number of minutes before a context that is not in RUNNING state will be automatically removed
     * from {@link ImportStore}. Set to zero, or to a negative number, to have no expiry. Defaults
     * to 1440, one day. Double data type is used to allow integration testing.
     */
    double getContextExpiration();

    /** @see #getContextExpiration() */
    void setContextExpiration(double contextExpiration);
}
