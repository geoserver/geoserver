/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import javax.xml.namespace.QName;

import net.opengis.wfsv.DifferenceQueryType;
import net.opengis.wfsv.WfsvFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.filter.Filter;


/**
 * Binding object for the type http://www.opengis.net/wfsv:DifferenceQueryType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *   &lt;xsd:complexType name=&quot;DifferenceQueryType&quot;&gt;
 *       &lt;xsd:sequence&gt;
 *           &lt;xsd:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; ref=&quot;ogc:Filter&quot;&gt;
 *               &lt;xsd:annotation&gt;
 *                   &lt;xsd:documentation&gt;
 *                   The Filter element is used to define spatial and/or non-spatial
 *                   constraints on query.  Spatial constrains use GML3 to specify
 *                   the constraining geometry.  A full description of the Filter
 *                   element can be found in the Filter Encoding Implementation
 *                   Specification.
 *                &lt;/xsd:documentation&gt;
 *               &lt;/xsd:annotation&gt;
 *           &lt;/xsd:element&gt;
 *       &lt;/xsd:sequence&gt;
 *       &lt;xsd:attribute name=&quot;typeName&quot; type=&quot;xsd:QName&quot; use=&quot;required&quot;&gt;
 *           &lt;xsd:annotation&gt;
 *               &lt;xsd:documentation&gt;
 *                 The typeName attribute is a single feature type name that indicates which type
 *                 of feature instances should be included in the reponse set.
 *                 The names must be a valid type that belong to this query's feature content
 *                 as defined by the GML Application Schema.
 *              &lt;/xsd:documentation&gt;
 *           &lt;/xsd:annotation&gt;
 *       &lt;/xsd:attribute&gt;
 *       &lt;xsd:attribute default=&quot;FIRST&quot; name=&quot;fromFeatureVersion&quot; type=&quot;xsd:string&quot;&gt;
 *           &lt;xsd:annotation&gt;
 *               &lt;xsd:documentation&gt;
 *                       Same as featureVersion in QueryType, but this indicates a starting feature version
 *                       for a difference and log operations.
 *                    &lt;/xsd:documentation&gt;
 *           &lt;/xsd:annotation&gt;
 *       &lt;/xsd:attribute&gt;
 *       &lt;xsd:attribute default=&quot;LAST&quot; name=&quot;toFeatureVersion&quot;
 *           type=&quot;xsd:string&quot; use=&quot;optional&quot;&gt;
 *           &lt;xsd:annotation&gt;
 *               &lt;xsd:documentation&gt;
 *                Same as featureVersion in QueryType, indicates the second version to be used for
 *                performing a difference of log operation.
 *             &lt;/xsd:documentation&gt;
 *           &lt;/xsd:annotation&gt;
 *       &lt;/xsd:attribute&gt;
 *   &lt;/xsd:complexType&gt;
 *
 * </code>
 *         </pre>
 *
 * </p>
 *
 * @generated
 */
public class DifferenceQueryTypeBinding extends AbstractComplexBinding {
    private WfsvFactory wfsvFactory;

    public DifferenceQueryTypeBinding(WfsvFactory wfsvFactory) {
        this.wfsvFactory = wfsvFactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFSV.DifferenceQueryType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return DifferenceQueryType.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        DifferenceQueryType result = wfsvFactory.createDifferenceQueryType();
        result.setTypeName((QName) node.getAttributeValue("typeName"));

        if (node.hasChild(Filter.class)) {
            result.setFilter((Filter) node.getChildValue(Filter.class));
        }

        if(node.hasAttribute("fromFeatureVersion"))
            result.setFromFeatureVersion((String) node.getAttributeValue("fromFeatureVersion"));
        if(node.hasAttribute("toFeatureVersion"))
            result.setToFeatureVersion((String) node.getAttributeValue("toFeatureVersion"));

        return result;
    }
}
