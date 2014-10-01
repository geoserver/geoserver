package org.geotools.tjs.bindings;


import net.opengis.tjs10.ColumnType1;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:Column1Type.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="ColumnType1" name="Column1Type"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="title" ref="tjs:Title"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="abstract" ref="tjs:Abstract"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;One or more paragraphs of human-readable text describing the attribute and suitable for display in a pop-up window.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="values" ref="tjs:Values"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Type of values and valid values for the contents of this column.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="getDataRequest" ref="tjs:GetDataRequest"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL reference to the GetData request for this column.  The request shall include any other columns  that describe or quantify the values in this attribute column (i.e. where the "purpose" of the column is SpatialComponentIdentifier, TemporalIdentfier, etc).&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="decimals" type="xsd:nonNegativeInteger"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Number of digits after the decimal, for decimal numbers with a fixed number of digits after the decimal.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="length" type="xsd:nonNegativeInteger" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Length of the field, in characters.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="name" type="xsd:string" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Name of the key field in the spatial framework dataset through which data can be joined.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="purpose" type="tjs:purposeType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Primary purpose of the attribute, indicating whether the column contains attribute data or a linkage key to some other spatial framework or nonspatial data table.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="type" type="tjs:typeType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Datatype, as defined by XML schema at http://www.w3.org/TR/xmlschema-2/#.&lt;/xsd:documentation&gt;
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
public class Column1TypeBinding extends AbstractComplexEMFBinding {

    public Column1TypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.Column1Type;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return ColumnType1.class;
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
