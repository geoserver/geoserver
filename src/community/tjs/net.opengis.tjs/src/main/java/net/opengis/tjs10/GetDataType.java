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
 * A representation of the model object '<em><b>Get Data Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.GetDataType#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataType#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataType#getAttributes <em>Attributes</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataType#getLinkageKeys <em>Linkage Keys</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataType#getFilterColumn <em>Filter Column</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataType#getFilterValue <em>Filter Value</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataType#getXSL <em>XSL</em>}</li>
 * <li>{@link net.opengis.tjs10.GetDataType#isAid <em>Aid</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='GetData_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getGetDataType()
 */
public interface GetDataType extends RequestBaseType {
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
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_FrameworkURI()
     */
    String getFrameworkURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#getFrameworkURI <em>Framework URI</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_DatasetURI()
     */
    String getDatasetURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#getDatasetURI <em>Dataset URI</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_Attributes()
     */
    String getAttributes();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#getAttributes <em>Attributes</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_LinkageKeys()
     */
    String getLinkageKeys();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#getLinkageKeys <em>Linkage Keys</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Linkage Keys</em>' attribute.
     * @generated
     * @see #getLinkageKeys()
     */
    void setLinkageKeys(String value);

    /**
     * Returns the value of the '<em><b>Filter Column</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The name of a Nominal or Ordinal field in the dataset upon which to filter the contents of the GetData response.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Filter Column</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='FilterColumn' namespace='##targetNamespace'"
     * @generated
     * @see #setFilterColumn(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_FilterColumn()
     */
    EObject getFilterColumn();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#getFilterColumn <em>Filter Column</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Filter Column</em>' containment reference.
     * @generated
     * @see #getFilterColumn()
     */
    void setFilterColumn(EObject value);

    /**
     * Returns the value of the '<em><b>Filter Value</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The Nominal or Ordinal value which the contents of the GetData response shall match.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Filter Value</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='FilterValue' namespace='##targetNamespace'"
     * @generated
     * @see #setFilterValue(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_FilterValue()
     */
    EObject getFilterValue();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#getFilterValue <em>Filter Value</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Filter Value</em>' containment reference.
     * @generated
     * @see #getFilterValue()
     */
    void setFilterValue(EObject value);

    /**
     * Returns the value of the '<em><b>XSL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Valid URL for an XSL document which will be referenced in the response XML in a fashion that it will be applied by web browsers.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>XSL</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='XSL' namespace='##targetNamespace'"
     * @generated
     * @see #setXSL(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_XSL()
     */
    EObject getXSL();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#getXSL <em>XSL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>XSL</em>' containment reference.
     * @generated
     * @see #getXSL()
     */
    void setXSL(EObject value);

    /**
     * Returns the value of the '<em><b>Aid</b></em>' attribute.
     * The default value is <code>"false"</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Boolean switch to request Attribute IDentifier.  If "aid=true" then an "aid" attribute will be included with each "V" element of  the response.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Aid</em>' attribute.
     * @model default="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
     * extendedMetaData="kind='attribute' name='aid'"
     * @generated
     * @see #isSetAid()
     * @see #unsetAid()
     * @see #setAid(boolean)
     * @see net.opengis.tjs10.Tjs10Package#getGetDataType_Aid()
     */
    boolean isAid();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetDataType#isAid <em>Aid</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Aid</em>' attribute.
     * @generated
     * @see #isSetAid()
     * @see #unsetAid()
     * @see #isAid()
     */
    void setAid(boolean value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.GetDataType#isAid <em>Aid</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetAid()
     * @see #isAid()
     * @see #setAid(boolean)
     */
    void unsetAid();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.GetDataType#isAid <em>Aid</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Aid</em>' attribute is set.
     * @generated
     * @see #unsetAid()
     * @see #isAid()
     * @see #setAid(boolean)
     */
    boolean isSetAid();

} // GetDataType
