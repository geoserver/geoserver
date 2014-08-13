/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>WSDL Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.WSDLType#getHref <em>Href</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='WSDL_._type' kind='empty'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getWSDLType()
 */
public interface WSDLType extends EObject {
    /**
     * Returns the value of the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The URL from which the WSDL document can be retrieved.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Href</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='href' namespace='http://www.w3.org/1999/xlink'"
     * @generated
     * @see #setHref(Object)
     * @see net.opengis.tjs10.Tjs10Package#getWSDLType_Href()
     */
    Object getHref();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.WSDLType#getHref <em>Href</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Href</em>' attribute.
     * @generated
     * @see #getHref()
     */
    void setHref(Object value);

} // WSDLType
