package org.geoserver.catalog;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.geotools.styling.*;
import org.geotools.util.Version;
import org.vfny.geoserver.util.SLDValidator;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class SLD10Handler extends SLDHandler {

    public static final Version VERSION = new Version("1.0.0");

    public SLD10Handler() {
        super(VERSION);
    }

//    @Override
//    public StyledLayerDescriptor parse(Object input) throws IOException {
//        SLDParser p = parser(input);
//        StyledLayerDescriptor sld = p.parseSLD();
//        if (sld.getStyledLayers().length == 0) {
//            //most likely a style that is not a valid sld, try to actually parse out a
//            // style and then wrap it in an sld
//            Style[] style = p.readDOM();
//            if (style.length > 0) {
//                NamedLayer l = styleFactory.createNamedLayer();
//                l.addStyle(style[0]);
//                sld.addStyledLayer(l);
//            }
//        }
//
//        return sld;
//    }
//
//    @Override
//    public List<Exception> validate(Object input) throws IOException {
//        return new SLDValidator().validateSLD(new InputSource(toReader(input)));
//    }
//
//    @Override
//    public void encode(StyledLayerDescriptor sld, boolean format, OutputStream output) throws IOException {
//        SLDTransformer tx = new SLDTransformer();
//        if (format) {
//            tx.setIndentation(2);
//        }
//        try {
//            tx.transform( sld, output );
//        }
//        catch (TransformerException e) {
//            throw (IOException) new IOException("Error writing style").initCause(e);
//        }
//    }
//
//
//    SLDParser parser(Object input) throws IOException {
//        if (input instanceof File) {
//            return new SLDParser(styleFactory, (File) input);
//        }
//        else {
//            return new SLDParser(styleFactory, toReader(input));
//        }
//    }

    @Override
    public StyledLayerDescriptor parse(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
            throws IOException {
        SLDParser p = parser(input, entityResolver);
        if (resourceLocator != null) {
            p.setOnLineResourceLocator(resourceLocator);
        }
        StyledLayerDescriptor sld = p.parseSLD();
        if (sld.getStyledLayers().length == 0) {
            //most likely a style that is not a valid sld, try to actually parse out a
            // style and then wrap it in an sld
            Style[] style = p.readDOM();
            if (style.length > 0) {
                NamedLayer l = styleFactory.createNamedLayer();
                l.addStyle(style[0]);
                sld.addStyledLayer(l);
            }
        }

        return sld;
    }

    @Override
    public List<Exception> validate(Object input, EntityResolver entityResolver) throws IOException {
        return new SLDValidator().validateSLD(new InputSource(toReader(input)));
    }

    @Override
    public void encode(StyledLayerDescriptor sld, boolean format, OutputStream output) throws IOException {
        SLDTransformer tx = new SLDTransformer();
        if (format) {
            tx.setIndentation(2);
        }
        try {
            tx.transform( sld, output );
        }
        catch (TransformerException e) {
            throw (IOException) new IOException("Error writing style").initCause(e);
        }
    }


    SLDParser parser(Object input, EntityResolver entityResolver) throws IOException {
        SLDParser parser;
        if (input instanceof File) {
            parser = new SLDParser(styleFactory, (File) input);
        }
        else {
            parser = new SLDParser(styleFactory, toReader(input));
        }

        parser.setEntityResolver(entityResolver);
        return parser;
    }
}
