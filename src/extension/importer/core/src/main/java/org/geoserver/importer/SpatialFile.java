/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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

import org.apache.commons.io.IOUtils;
import org.geotools.referencing.CRS;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resource.Type;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SpatialFile extends FileData {
    
    static EPSGCodeLookupCache EPSG_LOOKUP_CACHE = new EPSGCodeLookupCache();

    /**
     * .prj file
     */
    Resource prjFile;

    /** supplementary files, like indexes, etc...  */
    List<Resource> suppFiles = new ArrayList<Resource>();

    @Deprecated
    public SpatialFile(File file) {
        this(Files.asResource(file));
    }
    
    public SpatialFile(Resource file) {
        super(file);
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

    public List<Resource> getSuppFiles() {
        return suppFiles;
    }

    public List<Resource> allFiles() {
        ArrayList<Resource> all = new ArrayList<Resource>();
        all.add(file);
        if (prjFile != null) {
            all.add(prjFile);
        }
        all.addAll(suppFiles);
        return all;
    }

    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        //round up all the files with the same name
        suppFiles = new ArrayList();
        prjFile = null;

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

            String ext = f.name().substring(baseName.length());
            // once the basename is stripped, extension(s) should be present
            if (ext.charAt(0) == '.') {
                if (".prj".equalsIgnoreCase(ext)) {
                    prjFile = f;
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
                final PrintStream printStream = new PrintStream(getPrjFile().out());
                printStream.print(epsgWKT);
                printStream.close();
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
        
        InputStream is = prj.in();
        String wkt = IOUtils.toString(is);
        is.close();
        try {
            return CRS.parseWKT(wkt);
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
