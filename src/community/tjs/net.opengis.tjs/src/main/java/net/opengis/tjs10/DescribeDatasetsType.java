/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Describe Datasets Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.DescribeDatasetsType#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.DescribeDatasetsType#getDatasetURI <em>Dataset URI</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='DescribeDatasets_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDescribeDatasetsType()
 */
public interface DescribeDatasetsType extends RequestBaseType {
    /**
     * Returns the value of the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI the spatial framework to which the attribute data must apply.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework URI</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='element' name='FrameworkURI' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getDescribeDatasetsType_FrameworkURI()
     */
    String getFrameworkURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DescribeDatasetsType#getFrameworkURI <em>Framework URI</em>}' attribute.
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
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='element' name='DatasetURI' namespace='##targetNamespace'"
     * @generated
     * @see #setDatasetURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getDescribeDatasetsType_DatasetURI()
     */
    String getDatasetURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DescribeDatasetsType#getDatasetURI <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Dataset URI</em>' attribute.
     * @generated
     * @see #getDatasetURI()
     */
    void setDatasetURI(String value);

} // DescribeDatasetsType
