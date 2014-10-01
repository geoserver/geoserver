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
 * A representation of the model object '<em><b>Get Data Request Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.GetDataRequestType#getHref <em>Href</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='GetDataRequest_._type' kind='empty'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getGetDataRequestType()
 */
public interface GetDataRequestType extends EObject {
    /**
     * Returns the value of the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Href</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Href</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI" required="true"
     * extendedMetaData="kind='attribute' name='href' namespace='http://www.w3.org/1999/xlink'"
     * @generated
     * @see #setHref(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataRequestType_Href()
     */
    String getHref();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataRequestType#getHref <em>Href</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Href</em>' attribute.
     * @generated
     * @see #getHref()
     */
    void setHref(String value);

} // GetDataRequestType
