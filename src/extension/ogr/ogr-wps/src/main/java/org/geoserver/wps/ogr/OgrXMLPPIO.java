/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.ogr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.response.Ogr2OgrOutputFormat;
import org.geoserver.wps.ppio.XMLPPIO;
import org.geotools.feature.FeatureCollection;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/** Process XML output parameter using ogr2ogr process */
public class OgrXMLPPIO extends XMLPPIO {

    private Ogr2OgrOutputFormat ogr2OgrOutputFormat;

    private Operation operation;

    private String fileExtension;

    public OgrXMLPPIO(
            String mimeType,
            String fileExtension,
            Ogr2OgrOutputFormat ogr2OgrOutputFormat,
            Operation operation) {
        super(
                FeatureCollectionType.class,
                FeatureCollection.class,
                mimeType,
                org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION);
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
    public PPIODirection getDirection() {
        return PPIODirection.ENCODING;
    }

    @Override
    public String getFileExtension() {
        return this.fileExtension;
    }

    @Override
    public void encode(Object value, ContentHandler handler) throws Exception {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encode(value, os);

        InputStream bis = new ByteArrayInputStream(os.toByteArray());
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();
        XMLReader parser = saxParser.getXMLReader();
        parser.setContentHandler(handler);
        parser.parse(new InputSource(bis));
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return null;
    }
}
