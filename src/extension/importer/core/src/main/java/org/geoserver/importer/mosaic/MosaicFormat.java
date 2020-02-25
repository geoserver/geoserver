/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.importer.GridFormat;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.util.URLs;

public class MosaicFormat extends GridFormat {

    public MosaicFormat() {
        super(ImageMosaicFormat.class);
    }

    @Override
    public CoverageStoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        MosaicIndex index = new MosaicIndex((Mosaic) data);
        index.write();

        CoverageStoreInfo store = super.createStore(data, workspace, catalog);
        store.setURL(URLs.fileToUrl(index.getFile()).toString());
        return store;
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {

        List<ImportTask> tasks = super.list(data, catalog, monitor);

        Mosaic m = (Mosaic) data;
        if (m.getTimeMode() != TimeMode.NONE) {
            // set up the time dimension object
            for (ImportTask task : tasks) {
                DimensionInfo dim = new DimensionInfoImpl();
                dim.setEnabled(true);
                dim.setAttribute("time");
                dim.setPresentation(DimensionPresentation.LIST);
                dim.setUnits("ISO8601"); // TODO: is there an enumeration for this?

                ResourceInfo r = task.getLayer().getResource();
                r.getMetadata().put(ResourceInfo.TIME, dim);
            }
        }

        return tasks;
    }
}
