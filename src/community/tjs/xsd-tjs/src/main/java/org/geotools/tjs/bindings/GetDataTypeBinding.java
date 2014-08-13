package org.geotools.tjs.bindings;


import net.opengis.tjs10.GetDataType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:GetDataType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="GetDataType" name="GetDataType"&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="tjs:RequestBaseType"&gt;
 *              &lt;xsd:sequence&gt;
 *                  &lt;xsd:element ecore:name="frameworkURI" ref="tjs:FrameworkURI"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;URI of the spatial framework.  Normally a resolvable URL or a URN.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="datasetURI" ref="tjs:DatasetURI"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;URI of the attribute dataset.  Normally a resolvable URL or a URN.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="attributes" minOccurs="0" ref="tjs:Attributes"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;The AttributeNames requested by the user, in comma-delimited format&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="linkageKeys" minOccurs="0" ref="tjs:LinkageKeys"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;The DatasetKey identifiers requested by the user.  Identifiers shall be in comma-delimited format, where ranges shall be indicated with a minimum value and maximum value separated by a dash ("-").  The same Identifier cannot be requested multiple times.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="filterColumn" form="qualified"
 *                      minOccurs="0" name="FilterColumn" type="xsd:anyType"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;The name of a Nominal or Ordinal field in the dataset upon which to filter the contents of the GetData response.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="filterValue" form="qualified"
 *                      minOccurs="0" name="FilterValue" type="xsd:anyType"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;The Nominal or Ordinal value which the contents of the GetData response shall match.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element ecore:name="xSL" form="qualified"
 *                      minOccurs="0" name="XSL" type="xsd:anyType"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;Valid URL for an XSL document which will be referenced in the response XML in a fashion that it will be applied by web browsers.&lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *              &lt;/xsd:sequence&gt;
 *              &lt;xsd:attribute default="false" name="aid" type="xsd:boolean"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;Boolean switch to request Attribute IDentifier.  If "aid=true" then an "aid" attribute will be included with each "V" element of  the response.&lt;/xsd:documentation&gt;
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
public class GetDataTypeBinding extends AbstractComplexEMFBinding {

    public GetDataTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.GetDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetDataType.class;
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
