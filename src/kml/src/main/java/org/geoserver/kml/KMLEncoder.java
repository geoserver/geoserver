/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.geoserver.platform.ServiceException;

import de.micromata.opengis.kml.v_2_2_0.Kml;

/**
 * Encodes a KML object onto an output stream
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class KMLEncoder {
    
    private JAXBContext context;

    public KMLEncoder() throws JAXBException {
        // this creation is expensive, do it once and cache it
        context = JAXBContext.newInstance((Kml.class));
    }

    public void encode(Kml kml, OutputStream output, KmlEncodingContext context) {
        try {
            createMarshaller().marshal(kml, output);
        } catch (JAXBException e) {
            throw new ServiceException(e);
        } finally {
            if(context != null) {
                context.closeIterators();
            }
        }
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // hmm... this one is nasty, without the reference implementation the prefixes
        // are going to be a bit ugly. Not a big deal, to solve look at
        // http://cglib.sourceforge.net/xref/samples/Beans.html
        // try {
        // Class.forName("com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper");
        // m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new JKD6PrefixMapper());
        // } catch(Exception e) {
        //
        // }

        return m;
    }
}
