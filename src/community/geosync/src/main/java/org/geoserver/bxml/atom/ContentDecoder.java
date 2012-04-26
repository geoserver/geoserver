package org.geoserver.bxml.atom;

import javax.xml.namespace.QName;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.base.SimpleDecoder;
import org.geoserver.bxml.wfs_1_1.DeleteElementTypeDecoder;
import org.geoserver.bxml.wfs_1_1.InsertElementTypeDecoder;
import org.geoserver.bxml.wfs_1_1.UpdateElementTypeDecoder;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.ContentImpl;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

/**
 * The Class ContentDecoder.
 * 
 * @author cfarina
 */
public class ContentDecoder extends SimpleDecoder<ContentImpl> {

    /** The choice. */
    private Decoder<EObject> choice;

    /**
     * Instantiates a new content decoder.
     */
    @SuppressWarnings("unchecked")
    public ContentDecoder() {
        super(Atom.content);
        this.choice = new ChoiceDecoder<EObject>(new DeleteElementTypeDecoder(),
                new UpdateElementTypeDecoder(), new InsertElementTypeDecoder());
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the content impl
     * @throws Exception
     *             the exception
     */
    @Override
    public ContentImpl decode(BxmlStreamReader r) throws Exception {
        final QName elementName = r.getElementName();
        Assert.isTrue(canHandle(r.getElementName()));
        r.require(EventType.START_ELEMENT, elementName.getNamespaceURI(),
                elementName.getLocalPart());

        ContentImpl contentImpl = new ContentImpl();

        contentImpl.setType(r.getAttributeValue(null, Atom.type.getLocalPart()));
        contentImpl.setSrc(r.getAttributeValue(null, Atom.source.getLocalPart()));

        r.nextTag();
        contentImpl.setValue(choice.decode(r));

        r.nextTag();
        r.require(EventType.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
        return contentImpl;
    }
}
