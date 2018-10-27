/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.wfs.WFSException;
import org.geotools.gml2.bindings.GML2ParsingUtils;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Binding object for the type http://www.opengis.net/wfs:QueryType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="QueryType"&gt;
 *                  &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *              The Query element is of type
 *              QueryType.
 *              &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *                      &lt;xsd:element
 *              maxOccurs="unbounded" minOccurs="0" ref="ogc:PropertyName"&gt;
 *                  &lt;xsd:annotation&gt;              &lt;xsd:documentation&gt;
 *                      The PropertyName element is used to specify one or
 *                      more                 properties of a feature whose
 *                      values are to be retrieved                 by a Web
 *                      Feature Service.
 *                      While a Web Feature Service should endeavour to
 *                      satisfy                 the exact request specified,
 *                      in some instance this may                 not be
 *                      possible.  Specifically, a Web Feature Service
 *                      must generate a valid GML2 response to a Query
 *                      operation.                 The schema used to
 *                      generate the output may include
 *                      properties that are mandatory.  In order that the
 *                      output                 validates, these mandatory
 *                      properties must be specified                 in the
 *                      request.  If they are not, a Web Feature Service
 *                      may add them automatically to the Query before
 *                      processing                 it.  Thus a client
 *                      application should, in general, be
 *                      prepared to receive more properties than it
 *                      requested.                  Of course, using the
 *                      DescribeFeatureType request, a client
 *                      application can determine which properties are
 *                      mandatory                 and request them in the
 *                      first place.              &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;        &lt;/xsd:element&gt;        &lt;xsd:element
 *              maxOccurs="1" minOccurs="0" ref="ogc:Filter"&gt;
 *                  &lt;xsd:annotation&gt;              &lt;xsd:documentation&gt;
 *                      The Filter element is used to define spatial and/or
 *                      non-spatial                 constraints on query.
 *                      Spatial constrains use GML2 to specify
 *                      the constraining geometry.  A full description of
 *                      the Filter                 element can be found in
 *                      the Filter Encoding Implementation
 *                      Specification.              &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;        &lt;/xsd:element&gt;      &lt;/xsd:sequence&gt;
 *          &lt;xsd:attribute name="handle" type="xsd:string" use="optional"/&gt;
 *          &lt;xsd:attribute name="typeName" type="xsd:QName" use="required"/&gt;
 *          &lt;xsd:attribute name="featureVersion" type="xsd:string"
 *          use="optional"&gt;         &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;               For systems that
 *                  implement versioning, the featureVersion
 *                  attribute is used to specify which version of a
 *                  particular               feature instance is to be
 *                  retrieved.  A value of ALL means               that all
 *                  versions should be retrieved.  An integer value
 *                  &apos;i&apos;, means that the ith version should be
 *                  retrieve if it               exists or the most recent
 *                  version otherwise.            &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;      &lt;/xsd:attribute&gt;    &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class QueryTypeBinding extends AbstractComplexBinding {
    /** Wfs Factory */
    WfsFactory wfsfactory;

    /** namespace mappings */
    NamespaceSupport namespaceSupport;

    public QueryTypeBinding(WfsFactory wfsfactory, NamespaceSupport namespaceSupport) {
        this.wfsfactory = wfsfactory;
        this.namespaceSupport = namespaceSupport;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.QUERYTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return QueryType.class;
    }

    public void initializeChildContext(
            ElementInstance childInstance, Node node, MutablePicoContainer context) {
        // if an srsName is set for this geometry, put it in the context for
        // children, so they can use it as well
        if (node.hasAttribute("srsName")) {
            try {
                CoordinateReferenceSystem crs = GML2ParsingUtils.crs(node);
                if (crs != null) {
                    context.registerComponentInstance(CoordinateReferenceSystem.class, crs);
                }
            } catch (Exception e) {
                throw new WFSException(e, "InvalidParameterValue");
            }
        }
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        QueryType queryType = wfsfactory.createQueryType();

        // <xsd:element maxOccurs="unbounded" minOccurs="0" ref="ogc:PropertyName">
        // JD:difference in spec here, moved from ogc:PropertyName to string
        List propertyNames = node.getChildValues(PropertyName.class);

        for (Iterator p = propertyNames.iterator(); p.hasNext(); ) {
            PropertyName propertyName = (PropertyName) p.next();
            queryType.getPropertyName().add(propertyName.getPropertyName());
        }

        // <xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter">
        Filter filter = (Filter) node.getChildValue(Filter.class);

        if (filter == null) {
            filter = (Filter) Filter.INCLUDE;
        }

        queryType.setFilter(filter);

        // <xsd:attribute name="handle" type="xsd:string" use="optional"/>
        queryType.setHandle((String) node.getAttributeValue("handle"));

        // <xsd:attribute name="typeName" type="xsd:QName" use="required"/>
        List typeNameList = new ArrayList();
        typeNameList.add(node.getAttributeValue("typeName"));
        queryType.setTypeName(typeNameList);

        // <xsd:attribute name="featureVersion" type="xsd:string" use="optional">
        queryType.setFeatureVersion((String) node.getAttributeValue("featureVersion"));

        // JD: even though reprojection is not supported in 1.0 we handle it
        // anyways
        // &lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
        if (node.hasAttribute("srsName")) {
            queryType.setSrsName(new URI((String) node.getAttributeValue("srsName")));
        }

        return queryType;
    }
}
