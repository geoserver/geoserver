/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.geoserver.platform.ServiceException;

/**
 * Encodes a KML object onto an output stream
 *
 * @author Andrea Aime - GeoSolutions
 */
public class KMLEncoder {

    private JAXBContext context;

    private Templates templates;

    public KMLEncoder() throws JAXBException, TransformerException {
        // this creation is expensive, do it once and cache it
        context = JAXBContext.newInstance((Kml.class));
        String xslt = getClass().getResource("icon_style_patch.xsl").toString();
        templates = TransformerFactory.newInstance().newTemplates(new StreamSource(xslt));
    }

    public void encode(Kml kml, OutputStream output, KmlEncodingContext context) {
        try {
            if ((context != null) && (context.getWms() == null)) {
                // No need to transform WFS KML.
                createMarshaller().marshal(kml, output);
            } else {
                Transformer transformer = templates.newTransformer();
                JAXBSource source = new JAXBSource(createMarshaller(), kml);
                transformer.transform(source, new StreamResult(output));
            }
        } catch (JAXBException | TransformerException e) {
            throw new ServiceException(e);
        } finally {
            if (context != null) {
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
