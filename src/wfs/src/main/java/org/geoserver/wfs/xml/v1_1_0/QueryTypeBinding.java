/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.net.URI;
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
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;

/**
 * Binding object for the type http://www.opengis.net/wfs:QueryType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="QueryType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              The Query element is of type QueryType.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:choice maxOccurs="unbounded" minOccurs="0"&gt;
 *              &lt;xsd:element ref="wfs:PropertyName"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                     The Property element is used to specify one or more
 *                     properties of a feature whose values are to be retrieved
 *                     by a Web Feature Service.
 *
 *                     While a Web Feature Service should endeavour to satisfy
 *                     the exact request specified, in some instance this may
 *                     not be possible.  Specifically, a Web Feature Service
 *                     must generate a valid GML3 response to a Query operation.
 *                     The schema used to generate the output may include
 *                     properties that are mandatory.  In order that the output
 *                     validates, these mandatory properties must be specified
 *                     in the request.  If they are not, a Web Feature Service
 *                     may add them automatically to the Query before processing
 *                     it.  Thus a client application should, in general, be
 *                     prepared to receive more properties than it requested.
 *
 *                     Of course, using the DescribeFeatureType request, a client
 *                     application can determine which properties are mandatory
 *                     and request them in the first place.
 *                  &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:element&gt;
 *              &lt;xsd:element ref="ogc:Function"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                     A function may be used as a select item in a query.
 *                     However, if a function is used, care must be taken
 *                     to ensure that the result type matches the type in the
 *
 *                  &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:element&gt;
 *          &lt;/xsd:choice&gt;
 *          &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                  The Filter element is used to define spatial and/or non-spatial
 *                  constraints on query.  Spatial constrains use GML3 to specify
 *                  the constraining geometry.  A full description of the Filter
 *                  element can be found in the Filter Encoding Implementation
 *                  Specification.
 *               &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:SortBy"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                  The SortBy element is used specify property names whose
 *                  values should be used to order (upon presentation) the
 *                  set of feature instances that satisfy the query.
 *               &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The handle attribute allows a client application
 *                 to assign a client-generated identifier for the
 *                 Query.  The handle is included to facilitate error
 *                 reporting.  If one Query in a GetFeature request
 *                 causes an exception, a WFS may report the handle
 *                 to indicate which query element failed.  If the a
 *                 handle is not present, the WFS may use other means
 *                 to localize the error (e.g. line numbers).
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="typeName" type="wfs:TypeNameListType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                The typeName attribute is a list of one or more
 *                feature type names that indicate which types
 *                of feature instances should be included in the
 *                reponse set.  Specifying more than one typename
 *                indicates that a join operation is being performed.
 *                All the names in the typeName list must be valid
 *                types that belong to this query's feature content
 *                as defined by the GML Application Schema.
 *             &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="featureVersion" type="xsd:string" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                For systems that implement versioning, the featureVersion
 *                attribute is used to specify which version of a particular
 *                feature instance is to be retrieved.  A value of ALL means
 *                that all versions should be retrieved.  An integer value
 *                'i', means that the ith version should be retrieve if it
 *                exists or the most recent version otherwise.
 *             &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                This attribute is used to specify a specific WFS-supported SRS
 *                that should be used for returned feature geometries.  The value
 *                may be the WFS StorageSRS value, DefaultRetrievalSRS value, or
 *                one of AdditionalSRS values.  If no srsName value is supplied,
 *                then the features will be returned using either the
 *                DefaultRetrievalSRS, if specified, and StorageSRS otherwise.
 *                For feature types with no spatial properties, this attribute
 *                must not be specified or ignored if it is specified.
 *             &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class QueryTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public QueryTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
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
        QueryType query = wfsfactory.createQueryType();

        // &lt;xsd:choice maxOccurs="unbounded" minOccurs="0"&gt;
        // &lt;xsd:element ref="wfs:PropertyName"&gt;
        if (node.hasChild("PropertyName")) {
            // HACK, stripping of namespace prefix
            for (Iterator p = node.getChildValues("PropertyName").iterator(); p.hasNext(); ) {
                Object property = p.next();
                String propertyName;
                if (property instanceof String) propertyName = (String) property;
                else propertyName = (String) ((PropertyName) property).getPropertyName();

                query.getPropertyName().add(propertyName);
            }
        }

        // &lt;xsd:element ref="ogc:Function"&gt;
        if (node.hasChild("Function")) {
            query.getFunction().add(node.getChildValues("Function"));
        }

        // &lt;/xsd:choice&gt;

        // &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter"&gt;
        if (node.hasChild(Filter.class)) {
            query.setFilter((Filter) node.getChildValue(Filter.class));
        }

        // &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:SortBy"&gt;
        if (node.hasChild(SortBy[].class)) {
            SortBy[] sortBy = (SortBy[]) node.getChildValue(SortBy[].class);

            for (int i = 0; i < sortBy.length; i++) query.getSortBy().add(sortBy[i]);
        }

        // &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("handle")) {
            query.setHandle((String) node.getAttributeValue("handle"));
        }

        // &lt;xsd:attribute name="typeName" type="wfs:TypeNameListType" use="required"&gt;
        query.setTypeName((List) node.getAttributeValue("typeName"));

        // &lt;xsd:attribute name="featureVersion" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("featureVersion")) {
            query.setFeatureVersion((String) node.getAttributeValue("featureVersion"));
        }

        // &lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
        if (node.hasAttribute("srsName")) {
            query.setSrsName((URI) node.getAttributeValue("srsName"));
        }

        if (node.hasChild("XlinkPropertyName")) {
            query.getXlinkPropertyName().addAll(node.getChildValues("XlinkPropertyName"));
        }

        return query;
    }
}
