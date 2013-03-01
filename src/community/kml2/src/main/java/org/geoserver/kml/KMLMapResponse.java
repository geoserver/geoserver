/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.AbstractMapResponse;

import de.micromata.opengis.kml.v_2_2_0.Kml;

public class KMLMapResponse extends AbstractMapResponse {

    private WMS wms;

    public KMLMapResponse(WMS wms) {
        super(KMLMap.class, (Set<String>) null);
        this.wms = wms;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        KMLMap kmlMap = (KMLMap) value;
        Kml kml = kmlMap.getKml();
        try {
            createMarshaller().marshal(kml, output);
        } catch (JAXBException e) {
            throw new ServiceException(e);
        } finally {
            kmlMap.dispose();
        }
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller m = JAXBContext.newInstance((Kml.class)).createMarshaller();
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
