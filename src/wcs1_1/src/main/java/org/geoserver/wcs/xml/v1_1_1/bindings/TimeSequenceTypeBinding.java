/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 *           (c) 2009 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_1_1.bindings;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import net.opengis.wcs11.TimePeriodType;
import net.opengis.wcs11.TimeSequenceType;
import net.opengis.wcs11.Wcs111Factory;

import org.geoserver.wcs.xml.v1_1_1.WCS;
import org.geotools.gml3.GML;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.Position;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Binding object for the type http://www.opengis.net/wcs:TimeSequenceType.
 * 
 * <p>
 * 
 * <pre>
 *       <code>
 *  &lt;complexType name=&quot;TimeSequenceType&quot;&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;An ordered sequence of time positions or intervals. The time positions and periods shall be ordered from the oldest to the newest. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;choice maxOccurs=&quot;unbounded&quot;&gt;
 *          &lt;element ref=&quot;gml:timePosition&quot;/&gt;
 *          &lt;element ref=&quot;wcs:timePeriod&quot;/&gt;
 *      &lt;/choice&gt;
 *  &lt;/complexType&gt; 
 *      
 * </code>
 *       </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class TimeSequenceTypeBinding extends AbstractComplexBinding {

    /**
     * @generated
     */
    public QName getTarget() {
        return WCS.TimeSequenceType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return TimeSequenceType.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {
        List<Node> timePositions = node.getChildren("timePosition");
        TimeSequenceType results = Wcs111Factory.eINSTANCE.createTimeSequenceType();
        
        if (timePositions != null && !timePositions.isEmpty()) {
            for (Node timePositionNode : timePositions) {
                Date positionDate = ((Position) timePositionNode.getValue()).getDate();
                results.getTimePosition().add(cvtToGmt(positionDate));
            }

            return results;
        } else {
            List<Node> timePeriods = node.getChildren("TimePeriod");
            if (timePeriods != null && !timePeriods.isEmpty()) {
                for (Node timePeriodNode : timePeriods) {
                    Instant begining = new DefaultInstant((Position) timePeriodNode.getChild("BeginPosition").getValue());
                    Instant ending = new DefaultInstant((Position) timePeriodNode.getChild("EndPosition").getValue());

                    //Period timePeriod = new DefaultPeriod(begining, ending);
                    TimePeriodType timePeriod = Wcs111Factory.eINSTANCE.createTimePeriodType();
                    Date beginPosition = cvtToGmt(begining.getPosition().getDate());
                    Date endPosition = cvtToGmt(ending.getPosition().getDate());
                    
                    timePeriod.setBeginPosition(beginPosition);
                    timePeriod.setEndPosition(endPosition);

                    results.getTimePeriod().add(timePeriod);
                }

                return results;
            }
        }
        
        return results;
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
        List timeSequence = (List) object;

        if (timeSequence == null) {
            value.appendChild(document.createElementNS(GML.NAMESPACE, GML.Null.getLocalPart()));
        }

        return null;
    }

    public Object getProperty(Object object, QName name) {
        List timeSequence = (List) object;

        if (timeSequence == null || timeSequence.isEmpty()) {
            return null;
        }

        if (name.getLocalPart().equals("timePeriod") && timeSequence.get(0) instanceof Period) {
            return timeSequence;
        }

        if (name.getLocalPart().equals("timePosition")
                && timeSequence.get(0) instanceof Position) {
            List<Position> result = new LinkedList<Position>();

            for (Position position : (List<Position>) timeSequence)
                result.add(position);

            return result;
        }

        return null;
    }
    
    /**
     * 
     * @param date
     * @return
     */
    private static Date cvtToGmt( Date date )
    {
       TimeZone tz = TimeZone.getDefault();
       Date ret = new Date( date.getTime() - tz.getRawOffset() );

       // if we are now in DST, back off by the delta.  Note that we are checking the GMT date, this is the KEY.
       if ( tz.inDaylightTime( ret ))
       {
          Date dstDate = new Date( ret.getTime() - tz.getDSTSavings() );

          // check to make sure we have not crossed back into standard time
          // this happens when we are on the cusp of DST (7pm the day before the change for PDT)
          if ( tz.inDaylightTime( dstDate ))
          {
             ret = dstDate;
          }
       }

       return ret;
    }
}
