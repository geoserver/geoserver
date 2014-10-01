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
 * A representation of the model object '<em><b>Attribute Data Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.AttributeDataType#getGetDataURL <em>Get Data URL</em>}</li>
 * <li>{@link net.opengis.tjs10.AttributeDataType#getGetDataXML <em>Get Data XML</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='AttributeData_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getAttributeDataType()
 */
public interface AttributeDataType extends EObject {
    /**
     * Returns the value of the '<em><b>Get Data URL</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL which returns a valid tjs 0.12 GetData response.  Note that this may be a tjs GetData request (via HTTP GET), a stored response to a GetData request, or a web process that returns content compliant with the GetData response schema.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Get Data URL</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI"
     * extendedMetaData="kind='element' name='GetDataURL' namespace='##targetNamespace'"
     * @generated
     * @see #setGetDataURL(String)
     * @see net.opengis.tjs10.Tjs10Package#getAttributeDataType_GetDataURL()
     */
    String getGetDataURL();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.AttributeDataType#getGetDataURL <em>Get Data URL</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Get Data URL</em>' attribute.
     * @generated
     * @see #getGetDataURL()
     */
    void setGetDataURL(String value);

    /**
     * Returns the value of the '<em><b>Get Data XML</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * GetData request in XML encoding, including the name of the tjs server to be queried.  Note that since XML encoding of the GetData request is optional for tjs servers, this choice should not be used unless it is known that the tjs server supports this request method.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Get Data XML</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='GetDataXML' namespace='##targetNamespace'"
     * @generated
     * @see #setGetDataXML(GetDataXMLType)
     * @see net.opengis.tjs10.Tjs10Package#getAttributeDataType_GetDataXML()
     */
    GetDataXMLType getGetDataXML();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.AttributeDataType#getGetDataXML <em>Get Data XML</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Get Data XML</em>' containment reference.
     * @generated
     * @see #getGetDataXML()
     */
    void setGetDataXML(GetDataXMLType value);

} // AttributeDataType
