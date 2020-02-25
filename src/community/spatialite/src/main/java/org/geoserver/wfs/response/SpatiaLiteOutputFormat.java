/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.spatialite.SpatiaLiteDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * WFS output format for a GetFeature operation in which the outputFormat is "spatialite". The
 * reference documentation for this format can be found in this link:
 *
 * @link:http://www.gaia-gis.it/spatialite/docs.html.
 *     <p>Based on CSVOutputFormat.java and ShapeZipOutputFormat.java from geoserver 2.2.x
 * @author Pablo Velazquez, Geotekne, info@geotekne.com
 * @author Jose Macchi, Geotekne, jmacchi@geotekne.com
 */
public class SpatiaLiteOutputFormat extends WFSGetFeatureOutputFormat {

    public SpatiaLiteOutputFormat(GeoServer gs) {
        super(gs, "SpatiaLite");
    }

    /** @return "application/x-sqlite3"; */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/zip";
        // return "application/x-sqlite3";
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {

        SpatiaLiteDataStoreFactory dsFactory = new SpatiaLiteDataStoreFactory();
        if (!dsFactory.isAvailable()) {
            throw new ServiceException(
                    "SpatiaLite support not avaialable, ensure all required "
                            + "native libraries are installed");
        }

        /** base location to temporally store spatialite database `es */
        File dbFile = File.createTempFile("spatialite", ".db");
        try {
            Map dbParams = new HashMap();
            dbParams.put(SpatiaLiteDataStoreFactory.DBTYPE.key, "spatialite");
            dbParams.put(SpatiaLiteDataStoreFactory.DATABASE.key, dbFile.getAbsolutePath());

            DataStore dataStore = dsFactory.createDataStore(dbParams);
            try {
                for (FeatureCollection fc : featureCollection.getFeatures()) {

                    SimpleFeatureType featureType = (SimpleFeatureType) fc.getSchema();
                    // create a feature type
                    dataStore.createSchema(featureType);

                    FeatureWriter fw =
                            dataStore.getFeatureWriterAppend(
                                    featureType.getTypeName(), Transaction.AUTO_COMMIT);

                    // Start populating the table: tbl_name.
                    SimpleFeatureIterator it = (SimpleFeatureIterator) fc.features();
                    while (it.hasNext()) {
                        SimpleFeature f = it.next();
                        SimpleFeature g = (SimpleFeature) fw.next();

                        for (AttributeDescriptor att :
                                f.getFeatureType().getAttributeDescriptors()) {
                            String attName = att.getLocalName();
                            g.setAttribute(attName, f.getAttribute(attName));
                        }
                        fw.write();
                    }
                }
            } finally {
                dataStore.dispose();
            }

            BufferedInputStream bin = new BufferedInputStream(new FileInputStream(dbFile));

            ZipOutputStream zout = new ZipOutputStream(output);
            zout.putNextEntry(new ZipEntry(getDbFileName(getFeature)));

            IOUtils.copy(bin, zout);
            zout.flush();
            zout.closeEntry();
        } finally {
            dbFile.delete();
        }
    }

    public String getCapabilitiesElementName() {
        return "SpatiaLite";
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return getDbFileName(operation) + ".zip";
    }

    String getDbFileName(Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);

        // check format options
        String outputFileName = (String) request.getFormatOptions().get("FILENAME");
        if (outputFileName == null) {
            outputFileName =
                    request.getQueries().get(0).getTypeNames().get(0).getLocalPart() + ".db";
        }

        return outputFileName;
    }
}
