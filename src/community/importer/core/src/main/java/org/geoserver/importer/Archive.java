/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.IOException;

public class Archive extends Directory {

    public Archive(File file) throws IOException {
        super(Directory.createFromArchive(file).getFile());
    }

}
