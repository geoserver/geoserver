package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.uri;
import static org.geoserver.gss.internal.atom.Atom.version;

import javax.xml.namespace.QName;

import org.geoserver.bxml.base.SimpleDecoder;
import org.geoserver.bxml.base.StringValueDecoder;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.GeneratorImpl;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

/**
 * The Class GeneratorDecoder.
 * 
 * @author cfarina
 */
public class GeneratorDecoder extends SimpleDecoder<GeneratorImpl> {

    /**
     * Instantiates a new generator decoder.
     */
    public GeneratorDecoder() {
        super(Atom.generator);
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the generator impl
     * @throws Exception
     *             the exception
     */
    @Override
    public GeneratorImpl decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, elemName.getNamespaceURI(), elemName.getLocalPart());
        final QName name = r.getElementName();
        Assert.isTrue(canHandle(name));

        GeneratorImpl generator = new GeneratorImpl();

        generator.setUri(r.getAttributeValue(null, uri.getLocalPart()));
        generator.setVersion(r.getAttributeValue(null, version.getLocalPart()));

        EventType event = r.next();

        if (!isEndElement(r) && EventType.VALUE_STRING == event) {
            generator.setValue(new StringValueDecoder().decode(r));
        }

        r.require(EventType.END_ELEMENT, elemName.getNamespaceURI(), elemName.getLocalPart());
        return generator;
    }

}
