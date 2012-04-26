package org.geoserver.bxml.wfs_1_1;

import static org.geoserver.wfs.xml.v1_1_0.WFS.UPDATE;

import java.util.Iterator;

import javax.xml.namespace.QName;

import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs.impl.WfsFactoryImpl;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.SetterDecoder;
import org.geoserver.bxml.base.SimpleDecoder;
import org.geoserver.bxml.feature.FeatureTypeUtil;
import org.geoserver.bxml.feature.PropertyDecoder;
import org.geoserver.bxml.filter_1_1.FilterDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

import com.google.common.collect.Iterators;

/**
 * The Class UpdateElementTypeDecoder.
 * 
 * @author cfarina
 */
public class UpdateElementTypeDecoder extends SimpleDecoder<EObject> {

    /** The factory. */
    private final WfsFactory factory;

    /**
     * Instantiates a new update element type decoder.
     */
    public UpdateElementTypeDecoder() {
        super(UPDATE);
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

        final UpdateElementType element = factory.createUpdateElementType();

        element.setTypeName(FeatureTypeUtil.buildFeatureTypeName(r, elementName));

        ChoiceDecoder<Object> choice = new ChoiceDecoder<Object>();

        choice.addOption(new SetterDecoder<Object>(new FilterDecoder(), element, "filter"));
        choice.addOption(new SetterDecoder<Object>(new PropertyDecoder(element.getTypeName()),
                element, "property"));

        SequenceDecoder<Object> seq = new SequenceDecoder<Object>(1, 1);
        seq.add(choice, 0, Integer.MAX_VALUE);

        r.nextTag();
        Iterator<Object> iterator = seq.decode(r);
        Iterators.toArray(iterator, Object.class);

        r.require(EventType.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
        return element;
    }

}
