package org.geoserver.catalog;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.sld.v1_1.SLDConfiguration;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.geotools.xml.Parser;
import org.xml.sax.EntityResolver;

public class SLD11Handler extends SLDHandler {

    public static final Version VERSION = new Version("1.1.0");

    public SLD11Handler() {
        super(VERSION);
    }

//    @Override
//    public StyledLayerDescriptor parse(Object input) throws IOException {
//        SLDConfiguration sld = new SLDConfiguration();
//        try {
//            return (StyledLayerDescriptor) new Parser(sld).parse(toReader(input));
//        }
//        catch(Exception e) {
//            if (e instanceof IOException) throw (IOException) e;
//            throw (IOException) new IOException().initCause(e);
//        }
//    }
//
//    @Override
//    public List<Exception> validate(Object input) throws IOException {
//        SLDConfiguration sld = new SLDConfiguration();
//        Parser p = new Parser(sld);
//        p.setValidating(true);
//
//        try {
//            p.parse(toReader(input));
//            return p.getValidationErrors();
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            List validationErrors = new ArrayList<Exception>(p.getValidationErrors());
//            validationErrors.add(0, e);
//            return validationErrors;
//        }
//    }
//
//    @Override
//    public void encode(StyledLayerDescriptor sld, boolean format, OutputStream output) throws IOException {
//        // TODO Auto-generated method stub
//    }

    @Override
    public StyledLayerDescriptor parse(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
        throws IOException {

        if (resourceLocator == null && input instanceof File) {
            // setup for resolution of relative paths
            final java.net.URL surl = DataUtilities.fileToURL((File) input);
            DefaultResourceLocator defResourceLocator = new DefaultResourceLocator();
            defResourceLocator.setSourceUrl(surl);
            resourceLocator = defResourceLocator;
        }

        final ResourceLocator locator = resourceLocator;
        SLDConfiguration sld;
        if (locator != null) {
            sld = new SLDConfiguration() {
                protected void configureContext(org.picocontainer.MutablePicoContainer container) {
                    container.registerComponentInstance(ResourceLocator.class, locator);
                };
            };
        }
        else {
            sld = new SLDConfiguration();
        }

        try {
            Parser parser = new Parser(sld);
            parser.setEntityResolver(entityResolver);
            if (resourceLocator != null) {

            }
            return (StyledLayerDescriptor) parser.parse(toReader(input));
        }
        catch(Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw (IOException) new IOException().initCause(e);
        }
    }

    @Override
    public List<Exception> validate(Object input, EntityResolver entityResolver) throws IOException {
        SLDConfiguration sld = new SLDConfiguration();
        Parser p = new Parser(sld);
        p.setValidating(true);
        p.setEntityResolver(entityResolver);

        try {
            p.parse(toReader(input));
            return p.getValidationErrors();
        }
        catch(Exception e) {
            e.printStackTrace();
            List validationErrors = new ArrayList<Exception>(p.getValidationErrors());
            validationErrors.add(0, e);
            return validationErrors;
        }
    }

    @Override
    public void encode(StyledLayerDescriptor sld, boolean format, OutputStream output) throws IOException {
        // TODO Auto-generated method stub
    }

}
