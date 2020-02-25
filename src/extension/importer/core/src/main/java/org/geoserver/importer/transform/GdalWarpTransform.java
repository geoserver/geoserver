/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.importer.DataFormat;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.gce.image.WorldImageFormat;

/**
 * Runs gdalwarp on a input raster file
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GdalWarpTransform extends AbstractCommandLinePreTransform implements RasterTransform {
    private static final long serialVersionUID = -6241844409161277128L;

    /** Checks if gdalwarp is available */
    public static boolean isAvailable() throws IOException {
        return new GdalWarpTransform(new ArrayList<String>()).checkAvailable();
    }

    public GdalWarpTransform(List<String> options) {
        super(options);
    }

    @Override
    public void apply(ImportTask task, ImportData data) throws Exception {
        // let the transform run
        super.apply(task, data);

        // see if we need to update the layer definition, we just changed the CRS after all
        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        String originalSRS = resource.getSRS();

        // do so only if it's a direct import
        if (layer.getId() != null || resource == null || resource.getCatalog() == null) {
            return;
        }

        DataFormat format = DataFormat.lookup(((FileData) data).getFile());
        List<ImportTask> tasks = format.list(data, resource.getCatalog(), new ProgressMonitor());
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        LayerInfo updatedLayer = tasks.get(0).getLayer();
        ResourceInfo updatedResource = updatedLayer.getResource();
        String updatedSRS = updatedResource.getSRS();
        // check if the layer srs is incompatible with the one we just reprojected to, update if
        // necessary
        if (originalSRS == null || (!originalSRS.equals(updatedSRS) && updatedSRS != null)) {
            resource.setSRS(updatedSRS);
            resource.setNativeCRS(updatedResource.getNativeCRS());
            resource.setNativeBoundingBox(updatedResource.getNativeBoundingBox());
            resource.setLatLonBoundingBox(updatedResource.getLatLonBoundingBox());
        }
    }

    @Override
    protected List<String> getReplacementTargetNames(ImportData data) throws IOException {
        File input = getInputFile(data);
        String name = input.getName();
        String baseName = FilenameUtils.getBaseName(name);
        String extension = FilenameUtils.getExtension(name);
        List<String> names = new ArrayList<>();
        names.add(name);
        names.add(baseName + ".prj");
        names.add(baseName + ".wld");
        try {
            // try to get a format specific world file extension
            Set<String> worldExtensions = WorldImageFormat.getWorldExtension(extension);
            for (String we : worldExtensions) {
                names.add(baseName + we);
            }
        } catch (IllegalArgumentException e) {
            // in case the file type is not recognized we get an exception. That's fine.
        }
        return Collections.singletonList(name);
    }

    @Override
    protected File getInputFile(ImportData data) throws IOException {
        if (data instanceof FileData) {
            FileData fd = (FileData) data;
            return fd.getFile();
        } else {
            throw new IOException("Can run gdalwarp only against file data");
        }
    }

    @Override
    protected File getExecutable() throws IOException {
        return getExecutableFromPath("gdalwarp");
    }

    protected List<String> getAvailabilityTestOptions() {
        return Collections.singletonList("--version");
    }
}
