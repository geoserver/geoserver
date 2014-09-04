/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_1_1.bindings;

import javax.xml.namespace.QName;

import org.geotools.gml3.v3_2.GML;
import org.geotools.temporal.object.DefaultPosition;
import org.geotools.util.SimpleInternationalString;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.temporal.Position;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Binding object for the type http://www.opengis.net/gml:TimePositionType.
 * 
 * <p>
 * 
 * <pre>
 *	 <code>
 *  &lt;complexType final=&quot;#all&quot; name=&quot;TimePositionType&quot;&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;
 *        Indeterminate time values are also allowed, as described in ISO 19108. The indeterminatePosition 
 *        attribute can be used alone or it can qualify a specific value for temporal position (e.g. before 
 *        2002-12, after 1019624400). For time values that identify position within a calendar, the 
 *        calendarEraName attribute provides the name of the calendar era to which the date is 
 *        referenced (e.g. the Meiji era of the Japanese calendar).
 *        &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;simpleContent&gt;
 *          &lt;extension base=&quot;gml:TemporalPositionType&quot;&gt;
 *              &lt;attribute name=&quot;calendarEraName&quot; type=&quot;string&quot; use=&quot;optional&quot;/&gt;
 *              &lt;attribute default=&quot;#ISO-8601&quot; name=&quot;frame&quot; type=&quot;anyURI&quot; use=&quot;optional&quot;/&gt;
 *              &lt;attribute name=&quot;indeterminatePosition&quot;
 *                  type=&quot;gml:TimeIndeterminateValueType&quot; use=&quot;optional&quot;/&gt;
 *          &lt;/extension&gt;
 *      &lt;/simpleContent&gt;
 *  &lt;/complexType&gt; 
 * 	
 * </code>
 *	 </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class TimePositionTypeBinding extends AbstractComplexBinding {

    /**
     * @generated
     */
    public QName getTarget() {
        return GML.TimePositionType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return Position.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {
        Position timePosition = new DefaultPosition(new SimpleInternationalString((String) value));
        return timePosition;
    }

    /*
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     */
    @Override
    public Element encode(Object object, Document document, Element value)
            throws Exception {
        Position timePosition = (Position) object;

        if (timePosition == null) {
            value.appendChild(document.createElementNS(GML.NAMESPACE, org.geotools.gml3.GML.Null.getLocalPart()));
        }

        value.appendChild(document.createTextNode(timePosition.getDateTime().toString()));
        return null;
    }

    public Object getProperty(Object object, QName name) {
        Position value = (Position) object;
        
        if (name.getLocalPart().equals("frame")) {
            return "ISO-8601";
        }
        
        if (name.getLocalPart().equals("calendarEraName")) {
            return null;
        }
        
        if (name.getLocalPart().equals("indeterminatePosition")) {
            return null;
        }
        
        return null;
    }
}
