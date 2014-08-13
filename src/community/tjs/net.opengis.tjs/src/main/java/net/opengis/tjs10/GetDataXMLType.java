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
 * A representation of the model object '<em><b>Get Data XML Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.GetDataXMLType#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataXMLType#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataXMLType#getAttributes <em>Attributes</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataXMLType#getLinkageKeys <em>Linkage Keys</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataXMLType#getGetDataHost <em>Get Data Host</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataXMLType#getLanguage <em>Language</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='GetDataXML_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getGetDataXMLType()
 */
public interface GetDataXMLType extends EObject {
    /**
     * Returns the value of the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI of the spatial framework.  Normally a resolvable URL or a URN.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework URI</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='FrameworkURI' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataXMLType_FrameworkURI()
     */
    String getFrameworkURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataXMLType#getFrameworkURI <em>Framework URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework URI</em>' attribute.
     * @generated
     * @see #getFrameworkURI()
     */
    void setFrameworkURI(String value);

    /**
     * Returns the value of the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI of the attribute dataset.  Normally a resolvable URL or a URN.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Dataset URI</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='DatasetURI' namespace='##targetNamespace'"
     * @generated
     * @see #setDatasetURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataXMLType_DatasetURI()
     */
    String getDatasetURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataXMLType#getDatasetURI <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Dataset URI</em>' attribute.
     * @generated
     * @see #getDatasetURI()
     */
    void setDatasetURI(String value);

    /**
     * Returns the value of the '<em><b>Attributes</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The AttributeNames requested by the user, in comma-delimited format
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Attributes</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='element' name='Attributes' namespace='##targetNamespace'"
     * @generated
     * @see #setAttributes(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataXMLType_Attributes()
     */
    String getAttributes();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataXMLType#getAttributes <em>Attributes</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Attributes</em>' attribute.
     * @generated
     * @see #getAttributes()
     */
    void setAttributes(String value);

    /**
     * Returns the value of the '<em><b>Linkage Keys</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The DatasetKey identifiers requested by the user.  Identifiers shall be in comma-delimited format, where ranges shall be indicated with a minimum value and maximum value separated by a dash ("-").  The same Identifier cannot be requested multiple times.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Linkage Keys</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='element' name='LinkageKeys' namespace='##targetNamespace'"
     * @generated
     * @see #setLinkageKeys(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataXMLType_LinkageKeys()
     */
    String getLinkageKeys();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataXMLType#getLinkageKeys <em>Linkage Keys</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Linkage Keys</em>' attribute.
     * @generated
     * @see #getLinkageKeys()
     */
    void setLinkageKeys(String value);

    /**
     * Returns the value of the '<em><b>Get Data Host</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Base URL of the tjs server to which the attached GetData request shall be passed.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Get Data Host</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI"
     * extendedMetaData="kind='attribute' name='getDataHost'"
     * @generated
     * @see #setGetDataHost(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataXMLType_GetDataHost()
     */
    String getGetDataHost();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataXMLType#getGetDataHost <em>Get Data Host</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Get Data Host</em>' attribute.
     * @generated
     * @see #getGetDataHost()
     */
    void setGetDataHost(String value);

    /**
     * Returns the value of the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Value of the language parameter to be included in the GetData request.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Language</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='attribute' name='language'"
     * @generated
     * @see #setLanguage(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataXMLType_Language()
     */
    String getLanguage();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataXMLType#getLanguage <em>Language</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Language</em>' attribute.
     * @generated
     * @see #getLanguage()
     */
    void setLanguage(String value);

} // GetDataXMLType
