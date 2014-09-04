/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 *           (c) 2009 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_1_1.bindings;

import java.net.URI;

import javax.xml.namespace.QName;

import org.geoserver.wcs.xml.v1_1_1.WCS;
import org.geotools.gml3.GML;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.reference.DefaultTemporalReferenceSystem;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.Position;
import org.opengis.temporal.TemporalReferenceSystem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Binding object for the type http://www.opengis.net/wcs:TimePeriodType.
 * 
 * <p>
 * 
 * <pre>
 *       <code>
 *  &lt;complexType name=&quot;TimePeriodType&quot;&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;This is a variation of the GML TimePeriod, which allows the beginning and end of a time-period to be expressed in short-form inline using the begin/endPosition element, which allows an identifiable TimeInstant to be defined simultaneously with using it, or by reference, using xlinks on the begin/end elements. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element name=&quot;beginPosition&quot; type=&quot;gml:TimePositionType&quot;/&gt;
 *          &lt;element name=&quot;endPosition&quot; type=&quot;gml:TimePositionType&quot;/&gt;
 *          &lt;element minOccurs=&quot;0&quot; name=&quot;timeResolution&quot; type=&quot;gml:TimeDurationType&quot;/&gt;
 *      &lt;/sequence&gt;
 *      &lt;attribute default=&quot;#ISO-8601&quot; name=&quot;frame&quot; type=&quot;anyURI&quot; use=&quot;optional&quot;/&gt;
 *  &lt;/complexType&gt; 
 *      
 * </code>
 *       </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class TimePeriodTypeBinding extends AbstractComplexBinding {

    /**
     * @generated
     */
    public QName getTarget() {
        return WCS.TimePeriodType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return Period.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {

        String frameName = ((URI) node.getAttributeValue("frame", "#ISO-8601")).toString();
        NamedIdentifier frameID = new NamedIdentifier(Citations.CRS, frameName);
        TemporalReferenceSystem frame = new DefaultTemporalReferenceSystem(frameID, null);

        Instant begining = new DefaultInstant((Position) node.getChild("BeginPosition").getValue());
        Instant ending = new DefaultInstant((Position) node.getChild("EndPosition").getValue());

        Period timePeriod = new DefaultPeriod(begining, ending);

        return timePeriod;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.xml.AbstractComplexBinding#encode(java.lang.Object,
     *      org.w3c.dom.Document, org.w3c.dom.Element)
     */
    @Override
    public Element encode(Object object, Document document, Element value)
            throws Exception {
        Period timePeriod = (Period) object;

        if (timePeriod == null) {
            value.appendChild(document.createElementNS(GML.NAMESPACE, GML.Null.getLocalPart()));
        }

        return null;
    }

    public Object getProperty(Object object, QName name) {
        Period timePeriod = (Period) object;

        if (timePeriod == null) {
            return null;
        }

        if (name.getLocalPart().equals("BeginPosition")) {
            return timePeriod.getBeginning().getPosition();
        }

        if (name.getLocalPart().equals("EndPosition")) {
            return timePeriod.getEnding().getPosition();
        }

        return null;
    }
}
