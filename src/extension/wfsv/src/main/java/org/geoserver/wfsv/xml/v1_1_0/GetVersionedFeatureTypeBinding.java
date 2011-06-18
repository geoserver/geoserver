/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import java.math.BigInteger;

import javax.xml.namespace.QName;

import net.opengis.wfs.QueryType;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfsv.GetVersionedFeatureType;
import net.opengis.wfsv.WfsvFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

/**
 * Binding object for the type
 * http://www.opengis.net/wfsv:GetVersionedFeatureType.
 * 
 * <p>
 * 
 * <pre>
 *	 <code>
 *  &lt;xsd:complexType name=&quot;GetVersionedFeatureType&quot;&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *          The GetVersionedFeature extends GetFeature by returning a collection
 *          of AbstractVersionedFeature, which do contain more information about
 *          the last change occurred on each feature.
 *        &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base=&quot;wfs:GetFeatureType&quot;/&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt; 
 * 	
 * </code>
 *	 </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class GetVersionedFeatureTypeBinding extends AbstractComplexBinding {
    private WfsvFactory wfsvFactory;

    public GetVersionedFeatureTypeBinding(WfsvFactory wfsvFactory) {
        this.wfsvFactory = wfsvFactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFSV.GetVersionedFeatureType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return GetVersionedFeatureTypeBinding.class;
    }
    
//    public int getExecutionMode() {
//        return Binding.OVERRIDE;
//    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {
        GetVersionedFeatureType getFeature = wfsvFactory.createGetVersionedFeatureType();

        // lt;xsd:element maxOccurs="unbounded" ref="wfs:Query"/&gt;
        getFeature.getQuery().addAll(node.getChildValues(QueryType.class));

        // &lt;xsd:attribute default="results" name="resultType"
        // type="wfs:ResultTypeType" use="optional"&gt;
        // Funny, it seems resultType is not being parsed, still a string...
        if (node.hasAttribute("resultType")) {
            Object rt = node.getAttributeValue("resultType");
            if ("results".equals(rt)) {
                getFeature.setResultType(ResultTypeType.RESULTS_LITERAL);
            } else if ("hits".equals(rt)) {
                getFeature.setResultType(ResultTypeType.HITS_LITERAL);
            } else {
                getFeature.setResultType((ResultTypeType) rt);
            }
        }
        
        if (node.hasAttribute("version")) {
            getFeature.setVersion((String) node
                    .getAttributeValue("version"));
        }

        // &lt;xsd:attribute default="text/xml; subtype=gml/3.1.1"
        // name="outputFormat" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("outputFormat")) {
            getFeature.setOutputFormat((String) node
                    .getAttributeValue("outputFormat"));
        } else if("1.0.0".equals(getFeature.getVersion())) {
            // use GML2 output by default for 1.0 requests
            getFeature.setOutputFormat("GML2");
        }

        // &lt;xsd:attribute name="maxFeatures" type="xsd:positiveInteger"
        // use="optional"&gt;
        if (node.hasAttribute("maxFeatures")) {
            getFeature.setMaxFeatures((BigInteger) node
                    .getAttributeValue("maxFeatures"));
        }

        // &lt;xsd:attribute name="traverseXlinkDepth" type="xsd:string"
        // use="optional"&gt;
        if (node.hasAttribute("traverseXlinkDepth")) {
            getFeature.setTraverseXlinkDepth((String) node
                    .getAttributeValue("traverseXlinkDepth"));
        }

        // &lt;xsd:attribute name="traverseXlinkExpiry"
        // type="xsd:positiveInteger" use="optional"&gt;
        if (node.hasAttribute("traverseXlinkExpiry")) {
            getFeature.setTraverseXlinkExpiry((BigInteger) node
                    .getAttributeValue("traverseXlinkExpiry"));
        }

        return getFeature;
    }

}