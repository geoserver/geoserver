package org.geoserver.bxml.atom;

import javax.xml.namespace.QName;

import org.geoserver.bxml.base.SimpleDecoder;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.CategoryImpl;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

/**
 * The Class CategoryDecoder.
 * 
 * @author cfarina
 */
public class CategoryDecoder extends SimpleDecoder<CategoryImpl> {

    /**
     * Instantiates a new category decoder.
     */
    public CategoryDecoder() {
        super(Atom.category);
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the category impl
     * @throws Exception
     *             the exception
     */
    @Override
    public CategoryImpl decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, elemName.getNamespaceURI(), elemName.getLocalPart());
        final QName name = r.getElementName();
        Assert.isTrue(canHandle(name));
        CategoryImpl category = new CategoryImpl();

        final String scheme = r.getAttributeValue(null, Atom.scheme.getLocalPart());
        final String term = r.getAttributeValue(null, Atom.term.getLocalPart());

        r.nextTag();

        category.setScheme(scheme);
        category.setTerm(term);

        return category;
    }
}
