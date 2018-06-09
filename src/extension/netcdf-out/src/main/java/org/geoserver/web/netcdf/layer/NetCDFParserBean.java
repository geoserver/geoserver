/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf.layer;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geotools.coverage.io.netcdf.cf.NetCDFCFParser;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.logging.Logging;

/** Bean used for creating a {@link NetCDFCFParser} singleton, parsing the input cf-standard file */
public class NetCDFParserBean {

    public static final String NETCDF_STANDARD_NAME_TABLE = "NETCDF_STANDARD_TABLE";

    public static final String NETCDF_STANDARD_NAME = "cf-standard-name-table.xml";

    private static final Logger LOGGER = Logging.getLogger(NetCDFParserBean.class);

    private NetCDFCFParser parser;

    public static Resource netcdfFile;

    static {
        // Check if an external file has been defined
        String tableName = System.getProperty(NETCDF_STANDARD_NAME_TABLE);
        if (tableName != null && !tableName.isEmpty() && tableName.endsWith("xml")) {
            File newFile = new File(tableName);
            // Check if the file is valid
            boolean valid = newFile.exists() && newFile.canRead() && newFile.isFile();
            // If the file is valid, use it has standard name table
            if (valid) {
                netcdfFile = Files.asResource(newFile);
            }
        }
    }

    public NetCDFParserBean() {
        Resource cfStandardTable = null;
        // Check if an external file has been defined
        if (netcdfFile != null) {
            cfStandardTable = netcdfFile;
        }
        // Checking if it is contained in the NetCDF Data Directory
        if (cfStandardTable == null && NetCDFUtilities.EXTERNAL_DATA_DIR != null) {
            // Getting the directory file
            File netCDFDir = new File(NetCDFUtilities.EXTERNAL_DATA_DIR);
            // Creating a File filter
            FileFilter filter =
                    FileFilterUtils.nameFileFilter(NETCDF_STANDARD_NAME, IOCase.INSENSITIVE);
            // Getting the filtered file array
            File[] files = netCDFDir.listFiles(filter);
            // Getting the file if present
            if (files != null && files.length > 0) {
                cfStandardTable = Files.asResource(files[0]);
            }
        }

        if (cfStandardTable == null) {
            // Getting geoServer data dir
            GeoServerDataDirectory datadir = GeoServerExtensions.bean(GeoServerDataDirectory.class);
            // Checking if the standard table is present
            try {
                cfStandardTable = datadir.get(NETCDF_STANDARD_NAME);
            } catch (IllegalStateException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        // Check if the file can be parsed
        if (cfStandardTable != null && cfStandardTable.getType() != Resource.Type.UNDEFINED) {
            NetCDFCFParser parser = null;
            try {
                parser = NetCDFCFParser.unmarshallXml(cfStandardTable.file());
            } catch (JAXBException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } catch (IllegalStateException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            // If so set it as global attribute
            if (parser != null) {
                this.parser = parser;
            }
        } else {
            LOGGER.log(Level.WARNING, "No CF-Standard File found");
        }
    }

    /**
     * @return an instance of {@link NetCDFCFParser} if present, or null if no cf-standard table
     *     file is present or badly parsed.
     */
    public NetCDFCFParser getParser() {
        return parser;
    }
}
