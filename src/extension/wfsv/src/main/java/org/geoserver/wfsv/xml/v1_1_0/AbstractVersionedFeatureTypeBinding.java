/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;


import javax.xml.namespace.QName;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

/**
 * Binding object for the type http://www.opengis.net/wfsv:AbstractVersionedFeatureType.
 *
 * <p>
 *	<pre>
 *	 <code>
 *  &lt;xsd:complexType abstract="true" name="AbstractVersionedFeatureType"&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="gml:AbstractFeatureType"&gt;
 *              &lt;xsd:sequence&gt;
 *                  &lt;xsd:element minOccurs="1" name="version" type="xsd:string"/&gt;
 *                  &lt;xsd:element minOccurs="0" name="author" type="xsd:string"/&gt;
 *                  &lt;xsd:element minOccurs="1" name="date" type="xsd:dateTime"/&gt;
 *                  &lt;xsd:element minOccurs="0" name="message" type="xsd:string"/&gt;
 *              &lt;/xsd:sequence&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt; 
 *		
 *	  </code>
 *	 </pre>
 * </p>
 *
 * @generated
 */
public class AbstractVersionedFeatureTypeBinding extends AbstractComplexBinding {

	/**
	 * @generated
	 */
	public QName getTarget() {
		return WFSV.AbstractVersionedFeatureType;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Class getType() {
		return null;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Object parse(ElementInstance instance, Node node, Object value) 
		throws Exception {
		
		//TODO: implement
		return null;
	}

}