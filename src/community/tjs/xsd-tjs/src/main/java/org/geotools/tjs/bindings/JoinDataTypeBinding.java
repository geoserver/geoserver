package org.geotools.tjs.bindings;


import net.opengis.tjs10.JoinDataType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:JoinDataType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="JoinDataType" name="JoinDataType"&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="tjs:RequestBaseType"&gt;
 *              &lt;xsd:sequence&gt;
 *                  &lt;xsd:element ecore:name="attributeData" form="qualified"
 *                      name="AttributeData" type="tjs:AttributeDataType"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;Attribute data to be joined to the spatial framework.  &lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="mapStyling" form="qualified"
 *                      minOccurs="0" name="MapStyling" type="tjs:MapStylingType"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;Styling that shall be applied if the AccessMechanisms of the requested output includes WMS.  If WMS is not supported, this element shall not be present.  If WMS is supported and this element is not present, a default styling will be applied to the WMS layer.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="classificationURL"
 *                      form="qualified" minOccurs="0"
 *                      name="ClassificationURL" type="xsd:anyType"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;URL that returns a file describing a data classification to be applied to the output (e.g. the classification to be used for a legend in the case where the output is a WMS). This file must be encoded in compliance with the XML Schema identified in the ClassificationSchemaURL element of the DescribeJoinAbilities response.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *              &lt;/xsd:sequence&gt;
 *              &lt;xsd:attribute name="update" type="tjs:updateType"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;Flag to indicate if the Rowset content would be used to update/replace any equivalent attribute data that currently exists on the server.&lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class JoinDataTypeBinding extends AbstractComplexEMFBinding {

    public JoinDataTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.JoinDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return JoinDataType.class;
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
