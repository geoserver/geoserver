/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import javax.xml.namespace.QName;

import net.opengis.wfsv.DifferenceQueryType;
import net.opengis.wfsv.GetDiffType;
import net.opengis.wfsv.WfsvFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;


/**
 * Binding object for the type http://www.opengis.net/wfsv:GetDiffType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:complexType name="GetDiffType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              A GetDiff element contains one or more DifferenceQuery elements
 *              that describe a difference query operation on one feature type.  In
 *              response to a GetDiff request, a Versioning Web Feature Service
 *              must be able to generate a Transaction command that can be used
 *              to alter features at fromFeatureVersion and alter them into features
 *              at toFeatureVersion
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="wfs:BaseRequestType"&gt;
 *              &lt;xsd:sequence&gt;
 *                  &lt;xsd:element maxOccurs="unbounded" ref="wfsv:DifferenceQuery"/&gt;
 *              &lt;/xsd:sequence&gt;
 *              &lt;xsd:attribute
 *                  default="application/xml; subtype=wfsv-transaction/1.1.0"
 *                  name="outputFormat" type="xsd:string" use="optional"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                       The outputFormat attribute is used to specify the output
 *                       format that the Versioning Web Feature Service should generate in
 *                       response to a GetDiff element.
 *                       The default value of 'application/xml; subtype=wfsv-transaction/1.1.0'
 *                       indicates that the output is an XML document that
 *                       conforms to the WFS 1.1.0 Transaction definition.
 *                       For the purposes of experimentation, vendor extension,
 *                       or even extensions that serve a specific community of
 *                       interest, other acceptable output format values may be
 *                       used to specify other formats as long as those values
 *                       are advertised in the capabilities document.
 *                    &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class GetDiffTypeBinding extends AbstractComplexBinding {
    private WfsvFactory wfsvFactory;

    public GetDiffTypeBinding(WfsvFactory wfsvFactory) {
        this.wfsvFactory = wfsvFactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFSV.GetDiffType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetDiffType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        GetDiffType result = wfsvFactory.createGetDiffType();
        result.getDifferenceQuery().addAll(node.getChildValues(DifferenceQueryType.class));

        if (node.hasAttribute("outputFormat")) {
            result.setOutputFormat((String) node.getAttributeValue("outputFormat"));
        }

        return result;
    }
}
