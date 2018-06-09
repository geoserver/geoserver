/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.gdal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.geoserver.wcs.response.GdalCoverageResponseDelegate;
import org.geoserver.wps.ppio.XMLPPIO;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gml4wcs.GML;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/** Encode XML based output parameter using gdal_translate command */
public class GdalXMLPPIO extends XMLPPIO {

    private GdalCoverageResponseDelegate delegate;
    private String outputFormat;
    private String fileExtension;

    protected GdalXMLPPIO(String outputFormat, GdalCoverageResponseDelegate delegate) {
        super(GridCoverage2D.class, GridCoverage2D.class, GML.RectifiedGridType);
        this.delegate = delegate;
        this.outputFormat = outputFormat;
        this.fileExtension = delegate.getFileExtension(outputFormat);
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

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        delegate.encode((GridCoverage2D) value, outputFormat, null, os);
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
}
