/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.wicket.model.Model;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

public class MosaicPanel extends SpatialFilePanel {

    public MosaicPanel(String id) {
        super(id);
    }

    @Override
    protected void initFileChooser(GeoServerFileChooser fileChooser) {
        fileChooser.setFilter(new Model((Serializable) DirectoryFileFilter.DIRECTORY));
    }

    @Override
    public ImportData createImportSource() throws IOException {
        return new Mosaic(new File(this.file));
    }
}
