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
import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.feature.FeatureCollection;

/** Process text based output parameter using ogr2ogr process */
public class OgrCDataPPIO extends CDataPPIO {

    private Ogr2OgrOutputFormat ogr2OgrOutputFormat;

    private String fileExtension;

    private Operation operation;

    public OgrCDataPPIO(
            String mimeType,
            String fileExtension,
            Ogr2OgrOutputFormat ogr2OgrOutputFormat,
            Operation operation) {
        super(FeatureCollectionType.class, FeatureCollection.class, mimeType);
        this.fileExtension = fileExtension;
        this.ogr2OgrOutputFormat = ogr2OgrOutputFormat;
        this.operation = operation;
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
    public Object decode(String input) throws Exception {
        return null;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return null;
    }
}
