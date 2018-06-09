package org.geoserver.wps.gs.download;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class JaxbPPIO extends ComplexPPIO {

    private final Class targetClass;
    private JAXBContext context;
    private EntityResolverProvider resolverProvider;

    public JaxbPPIO(Class targetClass, EntityResolverProvider resolverProvider)
            throws JAXBException, TransformerException {
        super(targetClass, targetClass, "text/xml");
        this.targetClass = targetClass;
        this.resolverProvider = resolverProvider;

        // this creation is expensive, do it once and cache it
        this.context = JAXBContext.newInstance((targetClass));
    }

    @Override
    public Object decode(Object input) throws Exception {
        if (input instanceof String) {
            return decode(new ByteArrayInputStream(((String) input).getBytes()));
        }
        return super.decode(input);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Unmarshaller unmarshaller = this.context.createUnmarshaller();

        EntityResolver resolver =
                resolverProvider != null ? resolverProvider.getEntityResolver() : null;
        if (resolver == null) {
            return unmarshaller.unmarshal(input);
        } else {
            // setup the entity resolver
            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            final XMLReader reader = saxParserFactory.newSAXParser().getXMLReader();
            reader.setEntityResolver(resolver);
            final SAXSource saxSource = new SAXSource(reader, new InputSource(input));

            return unmarshaller.unmarshal(saxSource);
        }
    }

    @Override
    public PPIODirection getDirection() {
        return PPIODirection.DECODING;
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        throw new UnsupportedOperationException();
        // this is the easy implementation, but requires tests to support it
        // this.context.createMarshaller().marshal(value, os);
    }
}
