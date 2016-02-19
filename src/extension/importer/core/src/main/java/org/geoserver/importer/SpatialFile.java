/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geotools.referencing.CRS;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resource.Type;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.annotation.Nullable;

public class SpatialFile extends FileData {
    
    private static final long serialVersionUID = -280215815681792790L;

    static EPSGCodeLookupCache EPSG_LOOKUP_CACHE = new EPSGCodeLookupCache();

    /**
     * .prj file
     */
    Resource prjFile;

    /**
     * style file
     */
    Resource styleFile;

    /** supplementary files, like indexes, etc...  */
    List<Resource> suppFiles = new ArrayList<Resource>();

    /**
     * Create from file system
     *  
     * @param file the spatial file
     *      * 
     * @Depecrated Use Resource instead of File
     */
    @Deprecated
    public SpatialFile(File file) {
        this(Files.asResource(file));
    }
    
    /**
     * Create from resource
     * 
     * @param resource the spatial resource
     */
    public SpatialFile(Resource resource) {
        super(resource);
    }

    public SpatialFile(SpatialFile other) {
        super(other);
        this.prjFile = other.getPrjFile();
        this.suppFiles.addAll(other.getSuppFiles());
    }

    public Resource getPrjFile() {
        return prjFile;
    }

    public void setPrjFile(Resource prjFile) {
        this.prjFile = prjFile;
    }

    public Resource getStyleFile() {
        return styleFile;
    }

    public void setStyleFile(Resource styleFile) {
        this.styleFile = styleFile;
    }

    public List<Resource> getSuppFiles() {
        return suppFiles;
    }

    public List<Resource> allFiles() {
        ArrayList<Resource> all = new ArrayList<Resource>();
        all.add(file);
        if (prjFile != null) {
            all.add(prjFile);
        }
        if (styleFile != null) {
            all.add(styleFile);
        }
        all.addAll(suppFiles);
        return all;
    }

    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        //round up all the files with the same name
        suppFiles = new ArrayList();
        prjFile = null;
        styleFile = null;

        final List<String> styleExtensions = Lists.transform(Styles.handlers(), new Function<StyleHandler, String>() {
            @Nullable
            @Override
            public String apply(@Nullable StyleHandler input) {
                return input.getFileExtension();
            }
        });

        // getBaseName only gets the LAST extension so beware for .shp.aux.xml stuff
        final String baseName = getBaseName(file.name());
        
        for (Resource f : file.parent().list()) {
            if (f.equals(file)) {
                continue;
            }
            
            if (!f.name().startsWith(baseName)) {
                continue;
            }

            if (f.getType() != Type.RESOURCE) {
                continue;
            }

            String ext = f.name().substring(baseName.length()).toLowerCase();

            // once the basename is stripped, extension(s) should be present
            if (ext.charAt(0) == '.') {
                if (".prj".equalsIgnoreCase(ext)) {
                    prjFile = f;
                }
                else if (styleFile == null && styleExtensions.contains(ext.substring(1))) {
                    // TODO: deal with multiple style files? for now we just grab the first
                    styleFile = f;
                }
                else {
                    suppFiles.add(f);
                }
            }
        }
        if (format == null) {
            format = DataFormat.lookup(file);
        }

        //fix the prj file (match to official epsg wkt)
        try {
            fixPrjFile();
        }
        catch(Exception e) {
            LOGGER.log(Level.WARNING, "Error fixing prj file", e);
        }
    }
    
    public void fixPrjFile() throws IOException {
        CoordinateReferenceSystem crs = readPrjToCRS();
        if (crs == null) {
            return;
        }

        try {
            CoordinateReferenceSystem epsgCrs = null;
            Integer epsgCode = EPSG_LOOKUP_CACHE.lookupEPSGCode(crs);
            if (epsgCode != null) {
                epsgCrs = CRS.decode("EPSG:" + epsgCode);
            }
            if (epsgCrs != null) {
                String epsgWKT = epsgCrs.toWKT();
                try (PrintStream printStream = new PrintStream(getPrjFile().out())) {
                    printStream.print(epsgWKT);
                }
            }
        }
        catch (FactoryException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
    
    public CoordinateReferenceSystem readPrjToCRS() throws IOException {
        Resource prj = getPrjFile();
        if (prj == null || !Resources.exists(prj)) {
            return null;
        }
        
        try (InputStream is = prj.in()) {
            return CRS.parseWKT(IOUtils.toString(is));
        }
        catch (Exception e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((suppFiles == null) ? 0 : suppFiles.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SpatialFile other = (SpatialFile) obj;
        if (suppFiles == null) {
            if (other.suppFiles != null)
                return false;
        } else if (!suppFiles.equals(other.suppFiles))
            return false;
        return true;
    }

    private Object readResolve() {
        suppFiles = suppFiles == null ? new ArrayList<Resource>() : suppFiles;
        return this;
    }
}
