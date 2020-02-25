/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import java.io.File;
import org.geoserver.util.IOUtils;

public class LiveData implements TestData {
    protected File source;

    protected File data;

    public LiveData(File dataDirSourceDirectory) {
        this.source = dataDirSourceDirectory;
    }

    /**
     * Deeps copy the dataDirSourceDirectory provided in the constructor into a temporary directory.
     * Subclasses may override it in order to add extra behavior (like setting up an external
     * database)
     */
    public void setUp() throws Exception {
        data = IOUtils.createRandomDirectory("./target", "live", "data");
        IOUtils.deepCopy(source, data);
    }

    public void tearDown() throws Exception {
        if (data != null) IOUtils.delete(data);
    }

    public File getDataDirectoryRoot() {
        return data;
    }

    public boolean isTestDataAvailable() {
        return true;
    }
}
