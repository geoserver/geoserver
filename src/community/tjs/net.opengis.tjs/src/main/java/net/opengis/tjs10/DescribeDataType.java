/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Describe Data Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.DescribeDataType#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.DescribeDataType#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.DescribeDataType#getAttributes <em>Attributes</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='DescribeData_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDescribeDataType()
 */
public interface DescribeDataType extends RequestBaseType {
    /**
     * Returns the value of the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI of the spatial framework to which the attribute data must apply.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework URI</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='element' name='FrameworkURI' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getDescribeDataType_FrameworkURI()
     */
    String getFrameworkURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DescribeDataType#getFrameworkURI <em>Framework URI</em>}' attribute.
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
     * URI of the dataset which contains the attributes to be described.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Dataset URI</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='element' name='DatasetURI' namespace='##targetNamespace'"
     * @generated
     * @see #setDatasetURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getDescribeDataType_DatasetURI()
     */
    String getDatasetURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DescribeDataType#getDatasetURI <em>Dataset URI</em>}' attribute.
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
     * The names of the attributes for which descriptions are requested from the server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Attributes</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='element' name='Attributes' namespace='##targetNamespace'"
     * @generated
     * @see #setAttributes(String)
     * @see net.opengis.tjs10.Tjs10Package#getDescribeDataType_Attributes()
     */
    String getAttributes();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DescribeDataType#getAttributes <em>Attributes</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Attributes</em>' attribute.
     * @generated
     * @see #getAttributes()
     */
    void setAttributes(String value);

} // DescribeDataType
