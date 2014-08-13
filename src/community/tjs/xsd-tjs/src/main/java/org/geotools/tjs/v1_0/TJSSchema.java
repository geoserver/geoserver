package org.geotools.tjs.v1_0;

import org.geotools.feature.NameImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.ComplexTypeImpl;
import org.geotools.feature.type.SchemaImpl;
import org.geotools.xs.XSSchema;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TJSSchema extends SchemaImpl {

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="DescribeFrameworksValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="DescribeFrameworks"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType DESCRIBEFRAMEWORKSVALUETYPE_TYPE = build_DESCRIBEFRAMEWORKSVALUETYPE_TYPE();

    private static AttributeType build_DESCRIBEFRAMEWORKSVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeFrameworksValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType name="AbstractType"/&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ABSTRACTTYPE_TYPE = build_ABSTRACTTYPE_TYPE();

    private static ComplexType build_ABSTRACTTYPE_TYPE() {
        ComplexType builtType;
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "AbstractType"), Collections.<PropertyDescriptor>emptyList(), false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ValueType1" name="Value1Type"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="identifier" ref="tjs:Identifier"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Text string found in the V elements of this attribute&lt;/xsd:documentation&gt;
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
     *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *      &lt;xsd:attribute name="color" type="xsd:anySimpleType"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Hex code for a color that is suggested for cartographic portrayal of this value.  E.g."CCFFCC"&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType VALUE1TYPE_TYPE = build_VALUE1TYPE_TYPE();

    private static ComplexType build_VALUE1TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Identifier"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "color"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Value1Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ClassesType1" name="Classes1Type"&gt;
     *      &lt;xsd:sequence&gt;
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
     *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="value" form="qualified"
     *              maxOccurs="unbounded" name="Value" type="tjs:Value1Type"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid (non-null) values.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType CLASSES1TYPE_TYPE = build_CLASSES1TYPE_TYPE();

    private static ComplexType build_CLASSES1TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             VALUE1TYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Value"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Classes1Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="NullType1" name="Null1Type"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="identifier" ref="tjs:Identifier"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Text string representing a null value, found in the V elements of this attribute&lt;/xsd:documentation&gt;
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
     *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *      &lt;xsd:attribute name="color" type="xsd:anySimpleType"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Hex code for a color that is suggested for cartographic portrayal of this null value.  E.g."CCFFCC"&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType NULL1TYPE_TYPE = build_NULL1TYPE_TYPE();

    private static ComplexType build_NULL1TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Identifier"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "color"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Null1Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType name="NominalOrdinalExceptions"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="null" form="qualified"
     *              maxOccurs="unbounded" name="Null" type="tjs:Null1Type"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid null values.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType NOMINALORDINALEXCEPTIONS_TYPE = build_NOMINALORDINALEXCEPTIONS_TYPE();

    private static ComplexType build_NOMINALORDINALEXCEPTIONS_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             NULL1TYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Null"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "NominalOrdinalExceptions"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="NominalType" name="NominalType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="classes" form="qualified" minOccurs="0"
     *              name="Classes" type="tjs:Classes1Type"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid nominal classes for this attribute.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="exceptions" form="qualified"
     *              minOccurs="0" name="Exceptions" type="tjs:NominalOrdinalExceptions"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid exception classes for this attribute.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType NOMINALTYPE_TYPE = build_NOMINALTYPE_TYPE();

    private static ComplexType build_NOMINALTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             CLASSES1TYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Classes"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             NOMINALORDINALEXCEPTIONS_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Exceptions"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "NominalType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="JoinDataValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="JoinData"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType JOINDATAVALUETYPE_TYPE = build_JOINDATAVALUETYPE_TYPE();

    private static AttributeType build_JOINDATAVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "JoinDataValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ValueType" name="ValueType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="identifier" ref="tjs:Identifier"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Text string found in the V elements of this attribute&lt;/xsd:documentation&gt;
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
     *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *      &lt;xsd:attribute name="color" type="xsd:anySimpleType"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Hex code for a color that is suggested for cartographic portrayal of this value.  E.g."CCFFCC"&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *      &lt;xsd:attribute name="rank" type="xsd:nonNegativeInteger" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Rank order of this value, from lowest = 1 to highest.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType VALUETYPE_TYPE = build_VALUETYPE_TYPE();

    private static ComplexType build_VALUETYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Identifier"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "color"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.NONNEGATIVEINTEGER_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "rank"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "ValueType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ClassesType" name="ClassesType"&gt;
     *      &lt;xsd:sequence&gt;
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
     *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="value" form="qualified"
     *              maxOccurs="unbounded" name="Value" type="tjs:ValueType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid (non-null) values.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType CLASSESTYPE_TYPE = build_CLASSESTYPE_TYPE();

    private static ComplexType build_CLASSESTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             VALUETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Value"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "ClassesType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="OrdinalType" name="OrdinalType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="classes" form="qualified" minOccurs="0"
     *              name="Classes" type="tjs:ClassesType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid ordinal classes for this attribute.  Should be included when "purpose" of this attribute is "Data".&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="exceptions" form="qualified"
     *              minOccurs="0" name="Exceptions" type="tjs:NominalOrdinalExceptions"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid exception classes for this attribute.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ORDINALTYPE_TYPE = build_ORDINALTYPE_TYPE();

    private static ComplexType build_ORDINALTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             CLASSESTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Classes"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             NOMINALORDINALEXCEPTIONS_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Exceptions"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "OrdinalType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="MechanismType" name="MechanismType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="identifier" ref="tjs:Identifier"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Name which uniquely identifies this type of access mechanism supported by this server.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="title" ref="tjs:Title"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Human-readable title which uniquely identifies the type of access mechanism supported by this server.  Must be suitable for display in a pick-list to a user.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="abstract" form="qualified"
     *              name="Abstract" type="xsd:string"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Human-readable description of the type of access mechanism, suitable for display to a user seeking information about this type of access mechanism.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="reference" form="qualified"
     *              name="Reference" type="xsd:anyURI"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL that defines the access mechanism. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType MECHANISMTYPE_TYPE = build_MECHANISMTYPE_TYPE();

    private static ComplexType build_MECHANISMTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Identifier"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Reference"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "MechanismType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType name="OutputMechanismsType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="mechanism" maxOccurs="unbounded" ref="tjs:Mechanism"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Mechanism by which the attribute data can be accessed once it has been joined to the spatial framework.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType OUTPUTMECHANISMSTYPE_TYPE = build_OUTPUTMECHANISMSTYPE_TYPE();

    private static ComplexType build_OUTPUTMECHANISMSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             MECHANISMTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Mechanism"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "OutputMechanismsType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="RequestServiceType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="TJS"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType REQUESTSERVICETYPE_TYPE = build_REQUESTSERVICETYPE_TYPE();

    private static AttributeType build_REQUESTSERVICETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "RequestServiceType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="DescribeJoinAbilitiesValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="DescribeJoinAbilities"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType DESCRIBEJOINABILITIESVALUETYPE_TYPE = build_DESCRIBEJOINABILITIESVALUETYPE_TYPE();

    private static AttributeType build_DESCRIBEJOINABILITIESVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeJoinAbilitiesValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="LanguagesType" name="LanguagesType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="language" maxOccurs="unbounded" ref="ows:Language"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Identifier of a language used by the data(set) contents. This language identifier shall be as specified in IETF RFC 4646. When this element is omitted, the language used is not identified. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType LANGUAGESTYPE_TYPE = build_LANGUAGESTYPE_TYPE();

    private static ComplexType build_LANGUAGESTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/ows/1.1", "Language"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "LanguagesType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="DescribeDataValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="DescribeData"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType DESCRIBEDATAVALUETYPE_TYPE = build_DESCRIBEDATAVALUETYPE_TYPE();

    private static AttributeType build_DESCRIBEDATAVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDataValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType ecore:name="VersionType2" name="versionType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration ecore:name="_1" value="1"/&gt;
     *          &lt;xsd:enumeration ecore:name="_10" value="1.0"/&gt;
     *          &lt;xsd:enumeration ecore:name="_100" value="1.0.0"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType VERSIONTYPE_TYPE = build_VERSIONTYPE_TYPE();

    private static AttributeType build_VERSIONTYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "versionType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType name="RequestBaseType"&gt;
     *      &lt;xsd:annotation&gt;
     *          &lt;xsd:documentation&gt;TJS operation request base, for all TJS operations except GetCapabilities. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation.&lt;/xsd:documentation&gt;
     *      &lt;/xsd:annotation&gt;
     *      &lt;xsd:attribute name="language" type="xsd:string"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Language requested by the client for all human readable text in the response.  Consists of a two or five character RFC 4646 language code.  Must map to a language supported by the server.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *      &lt;xsd:attribute ecore:default="TJS" name="service"
     *          type="xsd:anySimpleType" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Service type identifier requested by the client.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *      &lt;xsd:attribute name="version" type="tjs:versionType"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Two-part version identifier requested by the client.  Must map to a version supported by the server.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType REQUESTBASETYPE_TYPE = build_REQUESTBASETYPE_TYPE();

    private static ComplexType build_REQUESTBASETYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "language"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "service"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             VERSIONTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "version"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "RequestBaseType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="DescribeKeyType" name="DescribeKeyType"&gt;
     *      &lt;xsd:complexContent&gt;
     *          &lt;xsd:extension base="tjs:RequestBaseType"&gt;
     *              &lt;xsd:sequence&gt;
     *                  &lt;xsd:element ecore:name="frameworkURI" ref="tjs:FrameworkURI"&gt;
     *                      &lt;xsd:annotation&gt;
     *                          &lt;xsd:documentation&gt;URI of the spatial framework to which the attribute data must apply.&lt;/xsd:documentation&gt;
     *                      &lt;/xsd:annotation&gt;
     *                  &lt;/xsd:element&gt;
     *              &lt;/xsd:sequence&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:complexContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DESCRIBEKEYTYPE_TYPE = build_DESCRIBEKEYTYPE_TYPE();

    private static ComplexType build_DESCRIBEKEYTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeKeyType"), schema, false,
                                               false, Collections.<Filter>emptyList(), REQUESTBASETYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="GetDataRequestType" name="GetDataRequestType"&gt;
     *      &lt;xsd:attribute ref="xlink:href" use="required"/&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType GETDATAREQUESTTYPE_TYPE = build_GETDATAREQUESTTYPE_TYPE();

    private static ComplexType build_GETDATAREQUESTTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.w3.org/1999/xlink", "href"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataRequestType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="StylingType" name="StylingType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="identifier" ref="tjs:Identifier"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Name that uniquely identifies this type of styling instructions supported by this server.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="title" ref="tjs:Title"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Human-readable title that uniquely identifies the type of styling instructions supported by this server.  Must be suitable for display in a pick-list to a user.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="abstract" form="qualified"
     *              name="Abstract" type="xsd:string"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Human-readable description of the type of styling instructions, suitable for display to a user seeking information about this type of styling instruction.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="reference" form="qualified"
     *              name="Reference" type="xsd:anyURI"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL that defines the styling instructions. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="schema" form="qualified" minOccurs="0"
     *              name="Schema" type="xsd:anyURI"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Reference to a definition of XML elements or types supported for this styling instruction (e.g., a URL which returns the XSD for SLD 1.0). This parameter shall be included when the styling instructions are XML encoded using an XML schema. When included, the input/output shall validate against the referenced XML Schema. This element shall be omitted if Schema does not apply to this form of styling instruction. Note: If this styling instruction uses a profile of a larger schema, the server administrator should provide that schema profile for validation purposes. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType STYLINGTYPE_TYPE = build_STYLINGTYPE_TYPE();

    private static ComplexType build_STYLINGTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Identifier"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Reference"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Schema"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "StylingType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType name="OutputStylingsType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="styling" maxOccurs="unbounded" ref="tjs:Styling"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Describes a form of styling instruction supported by this server. (e.g. SLD)&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType OUTPUTSTYLINGSTYPE_TYPE = build_OUTPUTSTYLINGSTYPE_TYPE();

    private static ComplexType build_OUTPUTSTYLINGSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             STYLINGTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Styling"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "OutputStylingsType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType ecore:name="TypeType" name="typeType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2String" value="http://www.w3.org/TR/xmlschema-2/#string"/&gt;
     *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Boolean" value="http://www.w3.org/TR/xmlschema-2/#boolean"/&gt;
     *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Integer" value="http://www.w3.org/TR/xmlschema-2/#integer"/&gt;
     *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Decimal" value="http://www.w3.org/TR/xmlschema-2/#decimal"/&gt;
     *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Float" value="http://www.w3.org/TR/xmlschema-2/#float"/&gt;
     *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Double" value="http://www.w3.org/TR/xmlschema-2/#double"/&gt;
     *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Datetime" value="http://www.w3.org/TR/xmlschema-2/#datetime"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType TYPETYPE_TYPE = build_TYPETYPE_TYPE();

    private static AttributeType build_TYPETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "typeType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ColumnType" name="ColumnType"&gt;
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
     *      &lt;xsd:attribute name="type" type="tjs:typeType" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Datatype, as defined by XML schema at http://www.w3.org/TR/xmlschema-2/#.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType COLUMNTYPE_TYPE = build_COLUMNTYPE_TYPE();

    private static ComplexType build_COLUMNTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.NONNEGATIVEINTEGER_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "decimals"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.NONNEGATIVEINTEGER_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "length"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "name"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             TYPETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "type"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "ColumnType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="FrameworkKeyType" name="FrameworkKeyType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="column" form="qualified"
     *              maxOccurs="unbounded" name="Column" type="tjs:ColumnType"/&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType FRAMEWORKKEYTYPE_TYPE = build_FRAMEWORKKEYTYPE_TYPE();

    private static ComplexType build_FRAMEWORKKEYTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             COLUMNTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Column"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKeyType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
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
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType BOUNDINGCOORDINATESTYPE_TYPE = build_BOUNDINGCOORDINATESTYPE_TYPE();

    private static ComplexType build_BOUNDINGCOORDINATESTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.DECIMAL_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "North"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.DECIMAL_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "South"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.DECIMAL_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "East"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.DECIMAL_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "West"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "BoundingCoordinatesType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="DescribeDatasetsRequestType" name="DescribeDatasetsRequestType"&gt;
     *      &lt;xsd:attribute ref="xlink:href" use="required"/&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DESCRIBEDATASETSREQUESTTYPE_TYPE = build_DESCRIBEDATASETSREQUESTTYPE_TYPE();

    private static ComplexType build_DESCRIBEDATASETSREQUESTTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.w3.org/1999/xlink", "href"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsRequestType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ReferenceDateType" name="ReferenceDateType"&gt;
     *      &lt;xsd:simpleContent&gt;
     *          &lt;xsd:extension base="xsd:string"&gt;
     *              &lt;xsd:attribute name="startDate" type="xsd:string"&gt;
     *                  &lt;xsd:annotation&gt;
     *                      &lt;xsd:documentation&gt;Start date of a range of time to which the framework/dataset applies.  Valid content is a date field of the form http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#gYear, gYearMonth, date, or dateTime.&lt;/xsd:documentation&gt;
     *                  &lt;/xsd:annotation&gt;
     *              &lt;/xsd:attribute&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:simpleContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType REFERENCEDATETYPE_TYPE = build_REFERENCEDATETYPE_TYPE();

    private static ComplexType build_REFERENCEDATETYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "startDate"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "ReferenceDateType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="DescribeDataRequestType" name="DescribeDataRequestType"&gt;
     *      &lt;xsd:attribute ref="xlink:href" use="required"/&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DESCRIBEDATAREQUESTTYPE_TYPE = build_DESCRIBEDATAREQUESTTYPE_TYPE();

    private static ComplexType build_DESCRIBEDATAREQUESTTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.w3.org/1999/xlink", "href"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDataRequestType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="DatasetType" name="DatasetType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="datasetURI" ref="tjs:DatasetURI"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URI of the attribute dataset.  Normally a resolvable URL or a URN.&lt;/xsd:documentation&gt;
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
     *          &lt;xsd:element ecore:name="describeDataRequest" ref="tjs:DescribeDataRequest"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL reference to the DescribeData request for this dataset.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DATASETTYPE_TYPE = build_DATASETTYPE_TYPE();

    private static ComplexType build_DATASETTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DatasetURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Organization"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             REFERENCEDATETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "ReferenceDate"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Version"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             DESCRIBEDATAREQUESTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDataRequest"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DatasetType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
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
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType FRAMEWORK4TYPE_TYPE = build_FRAMEWORK4TYPE_TYPE();

    private static ComplexType build_FRAMEWORK4TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Organization"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             REFERENCEDATETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "ReferenceDate"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Version"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             FRAMEWORKKEYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKey"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             BOUNDINGCOORDINATESTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "BoundingCoordinates"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             DESCRIBEDATASETSREQUESTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsRequest"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             DATASETTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Dataset"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Framework4Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="FrameworkType2" name="Framework2Type"&gt;
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
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType FRAMEWORK2TYPE_TYPE = build_FRAMEWORK2TYPE_TYPE();

    private static ComplexType build_FRAMEWORK2TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Organization"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             REFERENCEDATETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "ReferenceDate"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Version"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             FRAMEWORKKEYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKey"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             BOUNDINGCOORDINATESTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "BoundingCoordinates"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             DESCRIBEDATASETSREQUESTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsRequest"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Framework2Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="WSDLType" name="WSDLType"&gt;
     *      &lt;xsd:attribute ref="xlink:href" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;The URL from which the WSDL document can be retrieved.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType WSDLTYPE_TYPE = build_WSDLTYPE_TYPE();

    private static ComplexType build_WSDLTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.w3.org/1999/xlink", "href"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "WSDLType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="DescribeDatasetsType" name="DescribeDatasetsType"&gt;
     *      &lt;xsd:complexContent&gt;
     *          &lt;xsd:extension base="tjs:RequestBaseType"&gt;
     *              &lt;xsd:sequence&gt;
     *                  &lt;xsd:element ecore:name="frameworkURI" minOccurs="0" ref="tjs:FrameworkURI"&gt;
     *                      &lt;xsd:annotation&gt;
     *                          &lt;xsd:documentation&gt;URI the spatial framework to which the attribute data must apply.&lt;/xsd:documentation&gt;
     *                      &lt;/xsd:annotation&gt;
     *                  &lt;/xsd:element&gt;
     *                  &lt;xsd:element ecore:name="datasetURI" minOccurs="0" ref="tjs:DatasetURI"&gt;
     *                      &lt;xsd:annotation&gt;
     *                          &lt;xsd:documentation&gt;URI of the attribute dataset.  Normally a resolvable URL or a URN.&lt;/xsd:documentation&gt;
     *                      &lt;/xsd:annotation&gt;
     *                  &lt;/xsd:element&gt;
     *              &lt;/xsd:sequence&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:complexContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DESCRIBEDATASETSTYPE_TYPE = build_DESCRIBEDATASETSTYPE_TYPE();

    private static ComplexType build_DESCRIBEDATASETSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DatasetURI"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsType"), schema, false,
                                               false, Collections.<Filter>emptyList(), REQUESTBASETYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="SectionsType"&gt;
     *      &lt;xsd:annotation&gt;
     *          &lt;xsd:documentation&gt;XML encoded identifier comma separated list of a standard MIME type, possibly a parameterized MIME type.
     *  Comma separated list of available ServiceMetadata root elements. &lt;/xsd:documentation&gt;
     *      &lt;/xsd:annotation&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:pattern value="(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes)(,(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes))*"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType SECTIONSTYPE_TYPE = build_SECTIONSTYPE_TYPE();

    private static AttributeType build_SECTIONSTYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "SectionsType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
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
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType GETDATATYPE_TYPE = build_GETDATATYPE_TYPE();

    private static ComplexType build_GETDATATYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DatasetURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Attributes"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "LinkageKeys"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FilterColumn"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FilterValue"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "XSL"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.BOOLEAN_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "aid"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataType"), schema, false,
                                               false, Collections.<Filter>emptyList(), REQUESTBASETYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType ecore:name="DataClassType" name="DataClassType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="nominal"/&gt;
     *          &lt;xsd:enumeration value="ordinal"/&gt;
     *          &lt;xsd:enumeration value="measure"/&gt;
     *          &lt;xsd:enumeration value="count"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType DATACLASSTYPE_TYPE = build_DATACLASSTYPE_TYPE();

    private static AttributeType build_DATACLASSTYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "DataClassType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="FrameworkType" name="FrameworkType"&gt;
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
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType FRAMEWORKTYPE_TYPE = build_FRAMEWORKTYPE_TYPE();

    private static ComplexType build_FRAMEWORKTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Organization"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             REFERENCEDATETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "ReferenceDate"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Version"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             FRAMEWORKKEYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKey"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             BOUNDINGCOORDINATESTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "BoundingCoordinates"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="SpatialFrameworksType" name="SpatialFrameworksType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="framework" form="qualified"
     *              maxOccurs="unbounded" name="Framework" type="tjs:FrameworkType"/&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType SPATIALFRAMEWORKSTYPE_TYPE = build_SPATIALFRAMEWORKSTYPE_TYPE();

    private static ComplexType build_SPATIALFRAMEWORKSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             FRAMEWORKTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Framework"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "SpatialFrameworksType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="FailedType" name="FailedType"/&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType FAILEDTYPE_TYPE = build_FAILEDTYPE_TYPE();

    private static ComplexType build_FAILEDTYPE_TYPE() {
        ComplexType builtType;
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "FailedType"), Collections.<PropertyDescriptor>emptyList(), false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="StatusType" name="StatusType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="accepted" form="qualified"
     *              minOccurs="0" name="Accepted" type="xsd:anyType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Indicates that this request has been accepted by the server, but has not yet completed. The contents of this human-readable text string is left open to definition by each server implementation, but is expected to include any messages the server may wish to let the clients know. Such information could include when completion is expected, or any warning conditions that may have been encountered. The client may display this text to a human user. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="completed" form="qualified"
     *              minOccurs="0" name="Completed" type="xsd:anyType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Indicates that this request has completed execution with at lease partial success. The contents of this human-readable text string is left open to definition by each server, but is expected to include any messages the server may wish to let the client know, such as how long the operation took to execute, or any warning conditions that may have been encountered. The client may display this text string to a human user. The client should make use of the presence of this element to trigger automated or manual access to the results of the operation.  If manual access is intended, the client should use the presence of this element to present the results as downloadable links to the user. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="failed" form="qualified" minOccurs="0"
     *              name="Failed" type="tjs:FailedType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Indicates that execution of the JoinData operation failed, and includes error information.  The client may display this text string to a human user.  The presence of this element indicates that the operation completely failed and no Outputs were produced.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *      &lt;xsd:attribute name="creationTime" type="xsd:anySimpleType" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;The time (UTC) that the JoinData operation finished.  If the operation is still in progress, this element shall contain the creation time of this document.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *      &lt;xsd:attribute ref="xlink:href" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;HTTP reference to location where current JoinDataResponse document is stored.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType STATUSTYPE_TYPE = build_STATUSTYPE_TYPE();

    private static ComplexType build_STATUSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Accepted"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Completed"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             FAILEDTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Failed"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "creationTime"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.w3.org/1999/xlink", "href"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "StatusType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="KType" name="KType"&gt;
     *      &lt;xsd:simpleContent&gt;
     *          &lt;xsd:extension base="xsd:string"&gt;
     *              &lt;xsd:attribute name="aid" type="xsd:anySimpleType"/&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:simpleContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType KTYPE_TYPE = build_KTYPE_TYPE();

    private static ComplexType build_KTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "aid"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "KType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="RowType" name="RowType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="k" maxOccurs="unbounded" ref="tjs:K"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Spatial Key for this row.  For the GetData response, when there is more than one "K" element they are ordered according to the same sequence as the "FrameworkKey" elements of the "Columnset" structure.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="title" minOccurs="0" ref="tjs:Title"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ROWTYPE_TYPE = build_ROWTYPE_TYPE();

    private static ComplexType build_ROWTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             KTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "K"), 1, 2147483647, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "RowType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="RowsetType" name="RowsetType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="row" form="qualified"
     *              maxOccurs="unbounded" name="Row" type="tjs:RowType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Database row structure.  Contains data for a feature found in the spatial framework.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ROWSETTYPE_TYPE = build_ROWSETTYPE_TYPE();

    private static ComplexType build_ROWSETTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ROWTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Row"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "RowsetType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType name="DescribeFrameworkKeyType"&gt;
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
     *          &lt;xsd:element ecore:name="rowset" form="qualified" name="Rowset" type="tjs:RowsetType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Database table structure. Ordered list of all the spatial features for the identified framework.  Row elements are in ascending order based on the contents of the Spatial Key (K).&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DESCRIBEFRAMEWORKKEYTYPE_TYPE = build_DESCRIBEFRAMEWORKKEYTYPE_TYPE();

    private static ComplexType build_DESCRIBEFRAMEWORKKEYTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Organization"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             REFERENCEDATETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "ReferenceDate"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Version"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             FRAMEWORKKEYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKey"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             BOUNDINGCOORDINATESTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "BoundingCoordinates"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ROWSETTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Rowset"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeFrameworkKeyType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="GetCapabilitiesValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="GetCapabilities"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType GETCAPABILITIESVALUETYPE_TYPE = build_GETCAPABILITIESVALUETYPE_TYPE();

    private static AttributeType build_GETCAPABILITIESVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "GetCapabilitiesValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="VType" name="VType"&gt;
     *      &lt;xsd:simpleContent&gt;
     *          &lt;xsd:extension base="xsd:string"&gt;
     *              &lt;xsd:attribute name="aid" type="xsd:string"&gt;
     *                  &lt;xsd:annotation&gt;
     *                      &lt;xsd:documentation&gt;Attribute identifier, namely the corresponding AttributeName &lt;/xsd:documentation&gt;
     *                  &lt;/xsd:annotation&gt;
     *              &lt;/xsd:attribute&gt;
     *              &lt;xsd:attribute default="false" name="null" type="xsd:boolean"&gt;
     *                  &lt;xsd:annotation&gt;
     *                      &lt;xsd:documentation&gt;Boolean value, when present and "true" indicates that this particular value is missing for some reason, and the contents of the element must be processed accordingly.  &lt;/xsd:documentation&gt;
     *                  &lt;/xsd:annotation&gt;
     *              &lt;/xsd:attribute&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:simpleContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType VTYPE_TYPE = build_VTYPE_TYPE();

    private static ComplexType build_VTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "aid"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.BOOLEAN_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "null"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "VType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="RowType1" name="Row1Type"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="k" maxOccurs="unbounded" ref="tjs:K"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Spatial Key for this row.  For the GetData response, when there is more than one "K" element they are ordered according to the same sequence as the "FrameworkKey" elements of the "Columnset" structure.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="v" form="qualified"
     *              maxOccurs="unbounded" name="V" type="tjs:VType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Value of a attribute (i.e. data) applicable to the spatial feature identified by the "K" elements of this row. When there is more than one "V" element, they are ordered according to the same sequence as the "Column" elements of the "Columnset" structure above.  When this value is null (indicated with the null attribute) an identification of the reason may be included in the content of this element.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ROW1TYPE_TYPE = build_ROW1TYPE_TYPE();

    private static ComplexType build_ROW1TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             KTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "K"), 1, 2147483647, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             VTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "V"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Row1Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="RowsetType1" name="Rowset1Type"&gt;
     *      &lt;xsd:annotation&gt;
     *          &lt;xsd:documentation&gt;Rowset type defines a section for a dataset. Rowset can be presented more than once. However the efficient use of Rowset will be once per GetData response &lt;/xsd:documentation&gt;
     *      &lt;/xsd:annotation&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="row" form="qualified"
     *              maxOccurs="unbounded" name="Row" type="tjs:Row1Type"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Dataset Row&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ROWSET1TYPE_TYPE = build_ROWSET1TYPE_TYPE();

    private static ComplexType build_ROWSET1TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ROW1TYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Row"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Rowset1Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="NullType" name="NullType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="identifier" ref="tjs:Identifier"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Text string representing a null value, found in the "V" elements of this attribute.&lt;/xsd:documentation&gt;
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
     *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType NULLTYPE_TYPE = build_NULLTYPE_TYPE();

    private static ComplexType build_NULLTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Identifier"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Title"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ABSTRACTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Abstract"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Documentation"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "NullType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType name="MeasureCountExceptions"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="null" form="qualified"
     *              maxOccurs="unbounded" name="Null" type="tjs:NullType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Valid null values.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType MEASURECOUNTEXCEPTIONS_TYPE = build_MEASURECOUNTEXCEPTIONS_TYPE();

    private static ComplexType build_MEASURECOUNTEXCEPTIONS_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             NULLTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Null"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "MeasureCountExceptions"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="AcceptVersionsType" name="AcceptVersionsType"&gt;
     *      &lt;xsd:annotation&gt;
     *          &lt;xsd:documentation&gt;When omitted, server shall return latest supported version. &lt;/xsd:documentation&gt;
     *      &lt;/xsd:annotation&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="version" form="qualified"
     *              maxOccurs="unbounded" name="Version" type="tjs:VersionType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Specification version for the TJS GetCapabilities operation. The string value shall contain one "version" value.  Version numbering is similar to OWS 1.1 except the version number contains only two non-negative integers separated by decimal points, in the form "x.y", where the integer x is the major version and y is the minor version.  Currently version "1.0" is the only valid value for this element. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ACCEPTVERSIONSTYPE_TYPE = build_ACCEPTVERSIONSTYPE_TYPE();

    private static ComplexType build_ACCEPTVERSIONSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             VERSIONTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Version"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "AcceptVersionsType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="DescribeDataType" name="DescribeDataType"&gt;
     *      &lt;xsd:complexContent&gt;
     *          &lt;xsd:extension base="tjs:RequestBaseType"&gt;
     *              &lt;xsd:sequence&gt;
     *                  &lt;xsd:element ecore:name="frameworkURI" minOccurs="0" ref="tjs:FrameworkURI"&gt;
     *                      &lt;xsd:annotation&gt;
     *                          &lt;xsd:documentation&gt;URI of the spatial framework to which the attribute data must apply.&lt;/xsd:documentation&gt;
     *                      &lt;/xsd:annotation&gt;
     *                  &lt;/xsd:element&gt;
     *                  &lt;xsd:element ecore:name="datasetURI" minOccurs="0" ref="tjs:DatasetURI"&gt;
     *                      &lt;xsd:annotation&gt;
     *                          &lt;xsd:documentation&gt;URI of the dataset which contains the attributes to be described.&lt;/xsd:documentation&gt;
     *                      &lt;/xsd:annotation&gt;
     *                  &lt;/xsd:element&gt;
     *                  &lt;xsd:element ecore:name="attributes" minOccurs="0" ref="tjs:Attributes"&gt;
     *                      &lt;xsd:annotation&gt;
     *                          &lt;xsd:documentation&gt;The names of the attributes for which descriptions are requested from the server.&lt;/xsd:documentation&gt;
     *                      &lt;/xsd:annotation&gt;
     *                  &lt;/xsd:element&gt;
     *              &lt;/xsd:sequence&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:complexContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DESCRIBEDATATYPE_TYPE = build_DESCRIBEDATATYPE_TYPE();

    private static ComplexType build_DESCRIBEDATATYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DatasetURI"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Attributes"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDataType"), schema, false,
                                               false, Collections.<Filter>emptyList(), REQUESTBASETYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
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
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType GETDATAXMLTYPE_TYPE = build_GETDATAXMLTYPE_TYPE();

    private static ComplexType build_GETDATAXMLTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "DatasetURI"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Attributes"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "LinkageKeys"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "getDataHost"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "language"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataXMLType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="AttributeDataType" name="AttributeDataType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="getDataURL" form="qualified"
     *              minOccurs="0" name="GetDataURL" type="xsd:anyURI"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL which returns a valid tjs 0.12 GetData response.  Note that this may be a tjs GetData request (via HTTP GET), a stored response to a GetData request, or a web process that returns content compliant with the GetData response schema.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="getDataXML" form="qualified"
     *              minOccurs="0" name="GetDataXML" type="tjs:GetDataXMLType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;GetData request in XML encoding, including the name of the tjs server to be queried.  Note that since XML encoding of the GetData request is optional for tjs servers, this choice should not be used unless it is known that the tjs server supports this request method.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType ATTRIBUTEDATATYPE_TYPE = build_ATTRIBUTEDATATYPE_TYPE();

    private static ComplexType build_ATTRIBUTEDATATYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataURL"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             GETDATAXMLTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataXML"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "AttributeDataType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="MapStylingType" name="MapStylingType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="stylingIdentifier" form="qualified"
     *              name="StylingIdentifier" type="xsd:anyType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Name that identifies the type of styling to be invoked.  Must be a styling Identifier listed in the DescribeJoinAbilities response.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="stylingURL" form="qualified"
     *              name="StylingURL" type="xsd:anyURI"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Reference to a web-accessible resource that contains the styling information to be applied. This attribute shall contain a URL from which this input can be electronically retrieved. &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType MAPSTYLINGTYPE_TYPE = build_MAPSTYLINGTYPE_TYPE();

    private static ComplexType build_MAPSTYLINGTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "StylingIdentifier"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYURI_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "StylingURL"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "MapStylingType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType ecore:name="UpdateType" name="updateType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="true"/&gt;
     *          &lt;xsd:enumeration value="false"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType UPDATETYPE_TYPE = build_UPDATETYPE_TYPE();

    private static AttributeType build_UPDATETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "updateType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
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
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType JOINDATATYPE_TYPE = build_JOINDATATYPE_TYPE();

    private static ComplexType build_JOINDATATYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             ATTRIBUTEDATATYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "AttributeData"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             MAPSTYLINGTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "MapStyling"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "ClassificationURL"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             UPDATETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "update"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "JoinDataType"), schema, false,
                                               false, Collections.<Filter>emptyList(), REQUESTBASETYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="AcceptLanguagesType"&gt;
     *      &lt;xsd:restriction base="xsd:string"/&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType ACCEPTLANGUAGESTYPE_TYPE = build_ACCEPTLANGUAGESTYPE_TYPE();

    private static AttributeType build_ACCEPTLANGUAGESTYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "AcceptLanguagesType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="GetDataValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="GetData"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType GETDATAVALUETYPE_TYPE = build_GETDATAVALUETYPE_TYPE();

    private static AttributeType build_GETDATAVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType ecore:name="PurposeType" name="purposeType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="SpatialComponentIdentifier"/&gt;
     *          &lt;xsd:enumeration value="SpatialComponentProportion"/&gt;
     *          &lt;xsd:enumeration value="SpatialComponentPercentage"/&gt;
     *          &lt;xsd:enumeration value="TemporalIdentifier"/&gt;
     *          &lt;xsd:enumeration value="TemporalValue"/&gt;
     *          &lt;xsd:enumeration value="VerticalIdentifier"/&gt;
     *          &lt;xsd:enumeration value="VerticalValue"/&gt;
     *          &lt;xsd:enumeration value="OtherSpatialIdentifier"/&gt;
     *          &lt;xsd:enumeration value="NonSpatialIdentifier"/&gt;
     *          &lt;xsd:enumeration value="Attribute"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType PURPOSETYPE_TYPE = build_PURPOSETYPE_TYPE();

    private static AttributeType build_PURPOSETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "purposeType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="DescribeFrameworksType" name="DescribeFrameworksType"&gt;
     *      &lt;xsd:complexContent&gt;
     *          &lt;xsd:extension base="tjs:RequestBaseType"&gt;
     *              &lt;xsd:sequence&gt;
     *                  &lt;xsd:element ecore:name="frameworkURI" minOccurs="0" ref="tjs:FrameworkURI"&gt;
     *                      &lt;xsd:annotation&gt;
     *                          &lt;xsd:documentation&gt;URI of a spatial framework to which the attribute data can be joined.&lt;/xsd:documentation&gt;
     *                      &lt;/xsd:annotation&gt;
     *                  &lt;/xsd:element&gt;
     *              &lt;/xsd:sequence&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:complexContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType DESCRIBEFRAMEWORKSTYPE_TYPE = build_DESCRIBEFRAMEWORKSTYPE_TYPE();

    private static ComplexType build_DESCRIBEFRAMEWORKSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkURI"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeFrameworksType"), schema, false,
                                               false, Collections.<Filter>emptyList(), REQUESTBASETYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType ecore:name="GaussianType" name="gaussianType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="true"/&gt;
     *          &lt;xsd:enumeration value="false"/&gt;
     *          &lt;xsd:enumeration value="unknown"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType GAUSSIANTYPE_TYPE = build_GAUSSIANTYPE_TYPE();

    private static AttributeType build_GAUSSIANTYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "gaussianType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="UncertaintyType" name="UncertaintyType"&gt;
     *      &lt;xsd:simpleContent&gt;
     *          &lt;xsd:extension base="xsd:string"&gt;
     *              &lt;xsd:attribute name="gaussian" type="tjs:gaussianType" use="required"&gt;
     *                  &lt;xsd:annotation&gt;
     *                      &lt;xsd:documentation&gt;Uncertainty is of a Gaussian form, and Independent and Identically Distributed (IID).&lt;/xsd:documentation&gt;
     *                  &lt;/xsd:annotation&gt;
     *              &lt;/xsd:attribute&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:simpleContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType UNCERTAINTYTYPE_TYPE = build_UNCERTAINTYTYPE_TYPE();

    private static ComplexType build_UNCERTAINTYTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             GAUSSIANTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "gaussian"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "UncertaintyType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ColumnType2" name="Column2Type"&gt;
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
     *      &lt;xsd:attribute name="type" type="tjs:typeType" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Datatype, as defined by XML schema at http://www.w3.org/TR/xmlschema-2/#.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType COLUMN2TYPE_TYPE = build_COLUMN2TYPE_TYPE();

    private static ComplexType build_COLUMN2TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.NONNEGATIVEINTEGER_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "decimals"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.NONNEGATIVEINTEGER_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "length"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.STRING_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "name"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             TYPETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "type"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "Column2Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="FrameworkKeyType1" name="FrameworkKey1Type"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="column" form="qualified"
     *              maxOccurs="unbounded" name="Column" type="tjs:Column2Type"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Identifies a column that is used to form the framework key.  Where more than one of these elements is present then all of these columns are required to join the data table to the spatial framework.  The order of these elements defines the order of the "K" elements in the Rowset/Row structures below.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *      &lt;xsd:attribute name="complete" type="xsd:anySimpleType" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Identifies if there is at least one record in the Attribute dataset for every record in the Framework dataset.  “true” indicates that this is the case. “false” indicates that some Keys in the Framework dataset cannot be found in the Attribute dataset.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *      &lt;xsd:attribute name="relationship" type="xsd:anySimpleType" use="required"&gt;
     *          &lt;xsd:annotation&gt;
     *              &lt;xsd:documentation&gt;Identifies if the relationship between the Framework and the Attribute datasets are 1:1 or 1:many.  “one” indicates that there is at most one record in the Attribute dataset for every key in the Framework dataset.  “many” indicates that there may be more than one record in the Attribute dataset for every key in the Framework dataset, in which case some preliminary processing is required before the join operation can take place.&lt;/xsd:documentation&gt;
     *          &lt;/xsd:annotation&gt;
     *      &lt;/xsd:attribute&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType FRAMEWORKKEY1TYPE_TYPE = build_FRAMEWORKKEY1TYPE_TYPE();

    private static ComplexType build_FRAMEWORKKEY1TYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             COLUMN2TYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Column"), 1, 2147483647, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "complete"), 0, 1, true, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "relationship"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKey1Type"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="DescribeDatasetsValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="DescribeDatasets"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType DESCRIBEDATASETSVALUETYPE_TYPE = build_DESCRIBEDATASETSVALUETYPE_TYPE();

    private static AttributeType build_DESCRIBEDATASETSVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ParameterType" name="ParameterType"&gt;
     *      &lt;xsd:simpleContent&gt;
     *          &lt;xsd:extension base="xsd:string"&gt;
     *              &lt;xsd:attribute name="name" type="xsd:anySimpleType" use="required"&gt;
     *                  &lt;xsd:annotation&gt;
     *                      &lt;xsd:documentation&gt;Identifier for this parameter as defined by the service delivering the output of the JoinData operation.  For a WMS output this attribute shall be populated with the string "layers", thus providing sufficient information to allow the client to construct the "Layers=" parameter of a GetMap request.&lt;/xsd:documentation&gt;
     *                  &lt;/xsd:annotation&gt;
     *              &lt;/xsd:attribute&gt;
     *          &lt;/xsd:extension&gt;
     *      &lt;/xsd:simpleContent&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType PARAMETERTYPE_TYPE = build_PARAMETERTYPE_TYPE();

    private static ComplexType build_PARAMETERTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYSIMPLETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "name"), 0, 1, true, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "ParameterType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ResourceType" name="ResourceType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="uRL" form="qualified" name="URL" type="xsd:anyType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;URL from which this resource can be electronically retrieved, or from which a document can be retrieved that indicates access details for the resource (such as a OGC Capabilities document).  For OGC web services this shall be the complete GetCapabilities URL.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="parameter" form="qualified"
     *              maxOccurs="unbounded" minOccurs="0" name="Parameter" type="tjs:ParameterType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Parameter that may need to be included the HTTP requests to a web service identified by the URL parameter above.  For a WMS output there shall be one occurance of this element, and it shall be populated with the name of the layer produced by the JoinData operation.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType RESOURCETYPE_TYPE = build_RESOURCETYPE_TYPE();

    private static ComplexType build_RESOURCETYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "URL"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             PARAMETERTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Parameter"), 0, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "ResourceType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="ExceptionReportType" name="ExceptionReportType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="exception" ref="ows:Exception"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Error encountered during processing that prevented successful production of this output.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType EXCEPTIONREPORTTYPE_TYPE = build_EXCEPTIONREPORTTYPE_TYPE();

    private static ComplexType build_EXCEPTIONREPORTTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             XSSchema.ANYTYPE_TYPE, new NameImpl("http://www.opengis.net/ows/1.1", "Exception"), 1, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "ExceptionReportType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="OutputType" name="OutputType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="mechanism" ref="tjs:Mechanism"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;The access mechanism by which the joined data has been made available.  &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="resource" form="qualified"
     *              minOccurs="0" name="Resource" type="tjs:ResourceType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Reference to a web-accessible resource that was created by the JoinData operation.  This element shall be populated once the output has been successfully produced.  Prior to that time the content of the subelements may be empty.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *          &lt;xsd:element ecore:name="exceptionReport" form="qualified"
     *              minOccurs="0" name="ExceptionReport" type="tjs:ExceptionReportType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Unordered list of one or more errors encountered during the JoinData operation for this output.  These Exception elements shall be interpreted by clients as being independent of one another (not hierarchical).  This element is populated when the production of this output did not succeed.&lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType OUTPUTTYPE_TYPE = build_OUTPUTTYPE_TYPE();

    private static ComplexType build_OUTPUTTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             MECHANISMTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Mechanism"), 1, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             RESOURCETYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Resource"), 0, 1, false, null
                          )
        );
        schema.add(
                          new AttributeDescriptorImpl(
                                                             EXCEPTIONREPORTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "ExceptionReport"), 0, 1, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "OutputType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:complexType ecore:name="JoinedOutputsType" name="JoinedOutputsType"&gt;
     *      &lt;xsd:sequence&gt;
     *          &lt;xsd:element ecore:name="output" form="qualified"
     *              maxOccurs="unbounded" name="Output" type="tjs:OutputType"&gt;
     *              &lt;xsd:annotation&gt;
     *                  &lt;xsd:documentation&gt;Unordered list of all the outputs that have been or will be produced by this operation.  &lt;/xsd:documentation&gt;
     *              &lt;/xsd:annotation&gt;
     *          &lt;/xsd:element&gt;
     *      &lt;/xsd:sequence&gt;
     *  &lt;/xsd:complexType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final ComplexType JOINEDOUTPUTSTYPE_TYPE = build_JOINEDOUTPUTSTYPE_TYPE();

    private static ComplexType build_JOINEDOUTPUTSTYPE_TYPE() {
        ComplexType builtType;
        List<PropertyDescriptor> schema = new ArrayList<PropertyDescriptor>();
        schema.add(
                          new AttributeDescriptorImpl(
                                                             OUTPUTTYPE_TYPE, new NameImpl("http://www.opengis.net/tjs/1.0", "Output"), 1, 2147483647, false, null
                          )
        );
        builtType = new ComplexTypeImpl(
                                               new NameImpl("http://www.opengis.net/tjs/1.0", "JoinedOutputsType"), schema, false,
                                               false, Collections.<Filter>emptyList(), XSSchema.ANYTYPE_TYPE, null
        );
        return builtType;
    }

    /**
     * <p>
     * <pre>
     *   <code>
     *  &lt;xsd:simpleType name="DescribeKeyValueType"&gt;
     *      &lt;xsd:restriction base="xsd:string"&gt;
     *          &lt;xsd:enumeration value="DescribeKey"/&gt;
     *      &lt;/xsd:restriction&gt;
     *  &lt;/xsd:simpleType&gt;
     *
     *    </code>
     *   </pre>
     * </p>
     *
     * @generated
     */
    public static final AttributeType DESCRIBEKEYVALUETYPE_TYPE = build_DESCRIBEKEYVALUETYPE_TYPE();

    private static AttributeType build_DESCRIBEKEYVALUETYPE_TYPE() {
        AttributeType builtType;
        builtType = new AttributeTypeImpl(
                                                 new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeKeyValueType"), java.lang.Object.class, false,
                                                 false, Collections.<Filter>emptyList(), XSSchema.STRING_TYPE, null
        );
        return builtType;
    }


    public TJSSchema() {
        super("http://www.opengis.net/tjs/1.0");

        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeFrameworksValueType"), DESCRIBEFRAMEWORKSVALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "AbstractType"), ABSTRACTTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Value1Type"), VALUE1TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Classes1Type"), CLASSES1TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Null1Type"), NULL1TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "NominalOrdinalExceptions"), NOMINALORDINALEXCEPTIONS_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "NominalType"), NOMINALTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "JoinDataValueType"), JOINDATAVALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "ValueType"), VALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "ClassesType"), CLASSESTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "OrdinalType"), ORDINALTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "MechanismType"), MECHANISMTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "OutputMechanismsType"), OUTPUTMECHANISMSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "RequestServiceType"), REQUESTSERVICETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeJoinAbilitiesValueType"), DESCRIBEJOINABILITIESVALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "LanguagesType"), LANGUAGESTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDataValueType"), DESCRIBEDATAVALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "versionType"), VERSIONTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "RequestBaseType"), REQUESTBASETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeKeyType"), DESCRIBEKEYTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataRequestType"), GETDATAREQUESTTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "StylingType"), STYLINGTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "OutputStylingsType"), OUTPUTSTYLINGSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "OutputStylingsType"), OUTPUTSTYLINGSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "typeType"), TYPETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "ColumnType"), COLUMNTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKeyType"), FRAMEWORKKEYTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "BoundingCoordinatesType"), BOUNDINGCOORDINATESTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsRequestType"), DESCRIBEDATASETSREQUESTTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "ReferenceDateType"), REFERENCEDATETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDataRequestType"), DESCRIBEDATAREQUESTTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DatasetType"), DATASETTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Framework4Type"), FRAMEWORK4TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Framework2Type"), FRAMEWORK2TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "WSDLType"), WSDLTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsType"), DESCRIBEDATASETSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "SectionsType"), SECTIONSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataType"), GETDATATYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DataClassType"), DATACLASSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkType"), FRAMEWORKTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "SpatialFrameworksType"), SPATIALFRAMEWORKSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "FailedType"), FAILEDTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "StatusType"), STATUSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "KType"), KTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "RowType"), ROWTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "RowsetType"), ROWSETTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeFrameworkKeyType"), DESCRIBEFRAMEWORKKEYTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "GetCapabilitiesValueType"), GETCAPABILITIESVALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "VType"), VTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Row1Type"), ROW1TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Rowset1Type"), ROWSET1TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "NullType"), NULLTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "MeasureCountExceptions"), MEASURECOUNTEXCEPTIONS_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "VersionType"), VERSIONTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "AcceptVersionsType"), ACCEPTVERSIONSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDataType"), DESCRIBEDATATYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataXMLType"), GETDATAXMLTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "AttributeDataType"), ATTRIBUTEDATATYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "MapStylingType"), MAPSTYLINGTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "updateType"), UPDATETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "JoinDataType"), JOINDATATYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "AcceptLanguagesType"), ACCEPTLANGUAGESTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "GetDataValueType"), GETDATAVALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "purposeType"), PURPOSETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeFrameworksType"), DESCRIBEFRAMEWORKSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "gaussianType"), GAUSSIANTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "UncertaintyType"), UNCERTAINTYTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "Column2Type"), COLUMN2TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "FrameworkKey1Type"), FRAMEWORKKEY1TYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeDatasetsValueType"), DESCRIBEDATASETSVALUETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "ParameterType"), PARAMETERTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "ResourceType"), RESOURCETYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "ExceptionReportType"), EXCEPTIONREPORTTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "OutputType"), OUTPUTTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "JoinedOutputsType"), JOINEDOUTPUTSTYPE_TYPE);
        put(new NameImpl("http://www.opengis.net/tjs/1.0", "DescribeKeyValueType"), DESCRIBEKEYVALUETYPE_TYPE);
    }

}
