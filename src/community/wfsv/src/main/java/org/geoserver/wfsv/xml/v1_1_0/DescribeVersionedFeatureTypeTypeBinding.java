/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import javax.xml.namespace.QName;

import net.opengis.wfsv.DescribeVersionedFeatureTypeType;
import net.opengis.wfsv.WfsvFactory;

import org.geoserver.wfs.xml.v1_0_0.WFSBindingUtils;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

/**
 * Binding object for the type
 * http://www.opengis.net/wfsv:DescribeFeatureTypeType.
 * 
 * <p>
 * 
 * <pre>
 *	 <code>
 *  &lt;xsd:complexType name=&quot;DescribeFeatureTypeType&quot;&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              Same as wfs:DescribeFeatureType, but with the option to output
 *              a versioned feature type instead of a plain one
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base=&quot;wfs:DescribeFeatureTypeType&quot;&gt;
 *              &lt;xsd:attribute default=&quot;true&quot; name=&quot;versioned&quot;
 *                  type=&quot;xsd:boolean&quot; use=&quot;optional&quot;&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                       If false, the output is the same as wfs:DescribeFeatureType,
 *                       if true on the contrary the generated feature type will descend
 *                       form wfsv:AbstractVersionedFeatureType
 *                    &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *          &lt;/xsd:extension&gt;
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
public class DescribeVersionedFeatureTypeTypeBinding extends AbstractComplexBinding {
    
    private WfsvFactory wfsvFactory;

    public DescribeVersionedFeatureTypeTypeBinding(WfsvFactory wfsvFactory) {
        this.wfsvFactory = wfsvFactory;
    }
    
    /**
     * @generated
     */
    public QName getTarget() {
        return WFSV.DescribeVersionedFeatureTypeType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return DescribeVersionedFeatureTypeType.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {
        DescribeVersionedFeatureTypeType describeFeatureType = wfsvFactory.createDescribeVersionedFeatureTypeType();

        WFSBindingUtils.service(describeFeatureType, node);
        WFSBindingUtils.version(describeFeatureType, node);
        WFSBindingUtils.outputFormat(describeFeatureType, node, "XMLSCHEMA");
        // crude hack to work around the fact we don't have a real wfsv-1.0 configuration...
        // we assume everybody will want only the XMLSCHEMA output format for 1.0.0...
        if("1.0.0".equals(describeFeatureType.getVersion()))
            describeFeatureType.setOutputFormat("XMLSCHEMA");

        describeFeatureType.getTypeName().addAll(node.getChildValues(QName.class));
        if(node.hasAttribute("versioned"))
            describeFeatureType.setVersioned(((Boolean) node.getAttributeValue("versioned")).booleanValue());

        return describeFeatureType;
    }

}