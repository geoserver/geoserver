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
}
