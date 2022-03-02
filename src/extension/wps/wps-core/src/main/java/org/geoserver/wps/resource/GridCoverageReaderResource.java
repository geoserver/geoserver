/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.File;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;

public class GridCoverageReaderResource implements WPSResource {

    Resource file;

    AbstractGridCoverage2DReader reader;

    String name;

    public GridCoverageReaderResource(AbstractGridCoverage2DReader reader, File file) {
        this(reader, Files.asResource(file));
    }

    public GridCoverageReaderResource(AbstractGridCoverage2DReader reader, Resource file) {
        this.file = file;
        this.name = file.path() + reader.getGridCoverageNames()[0];
        this.reader = reader;
    }

    @Override
    public void delete() {
        try {
            reader.dispose();
        } finally {
            file.delete();
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
