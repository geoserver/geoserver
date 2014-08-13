package org.geotools.tjs.bindings;


import net.opengis.tjs10.GetDataXMLType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:GetDataXMLType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="GetDataXMLType" name="GetDataXMLType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="frameworkURI" ref="tjs:FrameworkURI"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URI of the spatial framework.  Normally a resolvable URL or a URN.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="datasetURI" ref="tjs:DatasetURI"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URI of the attribute dataset.  Normally a resolvable URL or a URN.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="attributes" minOccurs="0" ref="tjs:Attributes"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;The AttributeNames requested by the user, in comma-delimited format&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="linkageKeys" minOccurs="0" ref="tjs:LinkageKeys"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;The DatasetKey identifiers requested by the user.  Identifiers shall be in comma-delimited format, where ranges shall be indicated with a minimum value and maximum value separated by a dash ("-").  The same Identifier cannot be requested multiple times.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="getDataHost" type="xsd:anyURI"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Base URL of the tjs server to which the attached GetData request shall be passed.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="language" type="xsd:string"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Value of the language parameter to be included in the GetData request.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class GetDataXMLTypeBinding extends AbstractComplexEMFBinding {

    public GetDataXMLTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.GetDataXMLType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetDataXMLType.class;
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
