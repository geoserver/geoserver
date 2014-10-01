package org.geotools.tjs.bindings;


import net.opengis.tjs10.FrameworkType4;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:Framework4Type.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="FrameworkType4" name="Framework4Type"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="frameworkURI" ref="tjs:FrameworkURI"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URI of the spatial framework.  Normally a resolvable URL or a URN.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="organization" ref="tjs:Organization"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Human-readable name of the organization responsible for maintaining this object.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="title" ref="tjs:Title"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="abstract" ref="tjs:Abstract"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;One or more paragraphs of human-readable relevant text suitable for display in a pop-up window.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="referenceDate" ref="tjs:ReferenceDate"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Point in time to which the Framework/Dataset applies.  If the startDate attribute is included then the contents of this element describes a range of time (from "startDate" to "ReferenceDate") to which the framework/dataset applies.  Valid content is a date field of the form http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#gYear, gYearMonth, date, or dateTime.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="version" ref="tjs:Version"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Version identifier for this Framework / Dataset.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="frameworkKey" ref="tjs:FrameworkKey"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Describes the common key field in the spatial framework dataset through which data can be joined.  The values of this key populate the 'Rowset/Row/K' elements in the GetData response.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="boundingCoordinates" ref="tjs:BoundingCoordinates"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Identifies the bounding coordinates of the spatial framework using the WGS84 CRS.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="describeDatasetsRequest" ref="tjs:DescribeDatasetsRequest"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL reference to the DescribeDatasets request for this framework.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="dataset" form="qualified"
 *              maxOccurs="unbounded" name="Dataset" type="tjs:DatasetType"/&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class Framework4TypeBinding extends AbstractComplexEMFBinding {

    public Framework4TypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.Framework4Type;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return FrameworkType4.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {

        //TODO: implement and remove call to super
        return super.parse(instance, node, value);
    }

}
