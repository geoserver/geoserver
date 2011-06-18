package org.geoserver.geosearch;

import java.io.IOException;
import java.io.OutputStream;

import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

public class JDOMRepresentation extends OutputRepresentation {
    Document myDocument;

    public JDOMRepresentation(Document d){
        this(d, MediaType.APPLICATION_XML);
    }

    public JDOMRepresentation(Document d, MediaType t){
        super(t);
        myDocument = d;
    }

    public void write(OutputStream outputStream){
        try{
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(myDocument, outputStream);
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}
