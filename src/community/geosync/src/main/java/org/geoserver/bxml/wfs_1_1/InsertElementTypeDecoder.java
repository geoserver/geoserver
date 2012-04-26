package org.geoserver.bxml.wfs_1_1;

import static org.geoserver.wfs.xml.v1_1_0.WFS.INSERT;

import java.util.Iterator;

import javax.xml.namespace.QName;

import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs.impl.WfsFactoryImpl;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.bxml.base.SimpleDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.util.Assert;

/**
 * The Class InsertElementTypeDecoder.
 * 
 * @author cfarina
 */
public class InsertElementTypeDecoder extends SimpleDecoder<EObject> {

    /** The factory. */
    private final WfsFactory factory;

    /**
     * Instantiates a new insert element type decoder.
     */
    public InsertElementTypeDecoder() {
        super(INSERT);
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

        final InsertElementType insertElement = factory.createInsertElementType();

        SimpleFeatureSequenceDecoder<SimpleFeature> sequenceDecoder = new SimpleFeatureSequenceDecoder<SimpleFeature>(
                1, 1);
        sequenceDecoder.add(new SimpleFeatureDecoder(), 1, Integer.MAX_VALUE);

        r.nextTag();
        Iterator<SimpleFeature> iterator = sequenceDecoder.decode(r);
        while (iterator.hasNext()) {
            SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
            insertElement.getFeature().add(simpleFeature);
        }

        r.require(EventType.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
        return insertElement;
    }

}
