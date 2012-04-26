package org.geoserver.bxml.wfs_1_1;

import static org.geoserver.wfs.xml.v1_1_0.WFS.DELETE;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs.impl.WfsFactoryImpl;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.bxml.base.SimpleDecoder;
import org.geoserver.bxml.feature.FeatureTypeUtil;
import org.geoserver.bxml.filter_1_1.FilterDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

/**
 * The Class DeleteElementTypeDecoder.
 * 
 * @author cfarina
 */
public class DeleteElementTypeDecoder extends SimpleDecoder<EObject> {

    /** The factory. */
    private final WfsFactory factory;

    /**
     * Instantiates a new delete element type decoder.
     */
    public DeleteElementTypeDecoder() {
        super(DELETE);
        factory = WfsFactoryImpl.eINSTANCE;
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the e object
     * @throws Exception
     *             the exception
     */
    @Override
    public EObject decode(BxmlStreamReader r) throws Exception {
        final QName elementName = r.getElementName();
        Assert.isTrue(canHandle(elementName));
        r.require(EventType.START_ELEMENT, elementName.getNamespaceURI(),
                elementName.getLocalPart());

        final DeleteElementType element = factory.createDeleteElementType();

        element.setTypeName(FeatureTypeUtil.buildFeatureTypeName(r, elementName));

        r.nextTag();

        element.setFilter(new FilterDecoder().decode(r));

        r.nextTag();

        r.require(EventType.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
        return element;
    }
}
