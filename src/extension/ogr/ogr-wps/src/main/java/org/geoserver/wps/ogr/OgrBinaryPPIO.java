/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.ogr;

import java.io.InputStream;
import java.io.OutputStream;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.response.Ogr2OgrOutputFormat;
import org.geoserver.wps.ppio.BinaryPPIO;
import org.geotools.feature.FeatureCollection;

/** Process binary output parameter using ogr2ogr process */
public class OgrBinaryPPIO extends BinaryPPIO {

    private Ogr2OgrOutputFormat ogr2OgrOutputFormat;

    private Operation operation;

    private String fileExtension;

    public OgrBinaryPPIO(
            String mimeType,
            String fileExtension,
            Ogr2OgrOutputFormat ogr2OgrOutputFormat,
            Operation operation) {
        super(FeatureCollectionType.class, FeatureCollection.class, mimeType);
        this.ogr2OgrOutputFormat = ogr2OgrOutputFormat;
        this.operation = operation;
        this.fileExtension = fileExtension;
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        FeatureCollection<?, ?> features = (FeatureCollection<?, ?>) value;
        FeatureCollectionType fc = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fc.getFeature().add(features);
        ogr2OgrOutputFormat.write(fc, os, this.operation);
    }

    @Override
    public String getFileExtension() {
        return this.fileExtension;
    }

    @Override
    public PPIODirection getDirection() {
        return PPIODirection.ENCODING;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return null;
    }
}
