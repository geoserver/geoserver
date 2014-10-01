package org.geotools.tjs.bindings;


import net.opengis.tjs10.BoundingCoordinatesType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:BoundingCoordinatesType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="BoundingCoordinatesType" name="BoundingCoordinatesType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="north" form="qualified" name="North" type="xsd:decimal"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;WGS84 latitude of the northernmost coordinate of the spatial framework.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="south" form="qualified" name="South" type="xsd:decimal"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;WGS84 latitude of the southernmost coordinate of the spatial framework.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="east" form="qualified" name="East" type="xsd:decimal"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;WGS84 longitude of the easternmost coordinate of the spatial framework.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="west" form="qualified" name="West" type="xsd:decimal"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;WGS84 longitude of the westernmost coordinate of the spatial framework.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class BoundingCoordinatesTypeBinding extends AbstractComplexEMFBinding {

    public BoundingCoordinatesTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.BoundingCoordinatesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return BoundingCoordinatesType.class;
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
