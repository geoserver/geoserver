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
 * A representation of the model object '<em><b>Columnset Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ColumnsetType#getFrameworkKey <em>Framework Key</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnsetType#getAttributes <em>Attributes</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Columnset_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getColumnsetType()
 */
public interface ColumnsetType extends EObject {
    /**
     * Returns the value of the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifies the columns of the data table found in the Rowset structure below that are used to join the table to the spatial framework identified in the Framework structure above.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework Key</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='FrameworkKey' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkKey(FrameworkKeyType1)
     * @see net.opengis.tjs10.Tjs10Package#getColumnsetType_FrameworkKey()
     */
    FrameworkKeyType1 getFrameworkKey();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnsetType#getFrameworkKey <em>Framework Key</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework Key</em>' containment reference.
     * @generated
     * @see #getFrameworkKey()
     */
    void setFrameworkKey(FrameworkKeyType1 value);

    /**
     * Returns the value of the '<em><b>Attributes</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifies the columns of the data table found in the Rowset structure below that contain data that can be joined to the spatial framework identified in the Framework structure above.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Attributes</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Attributes' namespace='##targetNamespace'"
     * @generated
     * @see #setAttributes(AttributesType)
     * @see net.opengis.tjs10.Tjs10Package#getColumnsetType_Attributes()
     */
    AttributesType getAttributes();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnsetType#getAttributes <em>Attributes</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Attributes</em>' containment reference.
     * @generated
     * @see #getAttributes()
     */
    void setAttributes(AttributesType value);

} // ColumnsetType
