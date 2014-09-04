/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.FactoryException;

import com.csvreader.CsvReader;

public class CSVFileState {

    private static CoordinateReferenceSystem DEFAULT_CRS() throws FactoryException {
        return CRS.decode("EPSG:4326");
    };

    private final File file;

    private final String typeName;

    private final CoordinateReferenceSystem crs;

    private final URI namespace;

    private final String dataInput;

    private volatile String[] headers = null;

    public CSVFileState(File file) {
        this(file, null, null, null);
    }

    public CSVFileState(File file, URI namespace) {
        this(file, namespace, null, null);
    }

    public CSVFileState(File file, URI namespace, String typeName, CoordinateReferenceSystem crs) {
        this.file = file;
        this.typeName = typeName;
        this.crs = crs;
        this.namespace = namespace;
        this.dataInput = null;
    }

    // used by unit tests
    public CSVFileState(String dataInput) {
        this(dataInput, null);
    }

    public CSVFileState(String dataInput, String typeName) {
        this.dataInput = dataInput;
        this.typeName = typeName;
        this.crs = null;
        this.namespace = null;
        this.file = null;
    }

    public URI getNamespace() {
        return namespace;
    }

    public File getFile() {
        return file;
    }

    public String getTypeName() {
        return typeName != null ? typeName : FilenameUtils.getBaseName(file.getPath());
    }

    public CoordinateReferenceSystem getCrs() {
        if (crs != null) {
            return crs;
        }

        try {
            return CSVFileState.DEFAULT_CRS();
        } catch (FactoryException e) {
            return null;
        }
    }

    public CsvReader openCSVReader() throws IOException {
        Reader reader;
        if (file != null) {
            reader = new BufferedReader(new FileReader(file));
        } else {
            reader = new StringReader(dataInput);
        }
        CsvReader csvReader = new CsvReader(reader);
        if (!csvReader.readHeaders()) {
            throw new IOException("Error reading csv headers");
        }
        return csvReader;
    }

    public String[] getCSVHeaders() {
        if (headers == null) {
            synchronized (this) {
                if (headers == null) {
                    headers = readCSVHeaders();
                }
            }
        }
        return headers;
    }

    private String[] readCSVHeaders() {
        CsvReader csvReader = null;
        try {
            csvReader = openCSVReader();
            return csvReader.getHeaders();
        } catch (IOException e) {
            throw new RuntimeException("Failure reading csv headers", e);
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }
    }
}
