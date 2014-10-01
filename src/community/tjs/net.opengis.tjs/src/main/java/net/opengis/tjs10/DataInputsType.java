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
 * A representation of the model object '<em><b>Data Inputs Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.DataInputsType#getFramework <em>Framework</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='DataInputs_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDataInputsType()
 */
public interface DataInputsType extends EObject {
    /**
     * Returns the value of the '<em><b>Framework</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Framework</em>' containment reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Framework</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Framework' namespace='##targetNamespace'"
     * @generated
     * @see #setFramework(FrameworkDatasetDescribeDataType)
     * @see net.opengis.tjs10.Tjs10Package#getDataInputsType_Framework()
     */
    FrameworkDatasetDescribeDataType getFramework();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DataInputsType#getFramework <em>Framework</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework</em>' containment reference.
     * @generated
     * @see #getFramework()
     */
    void setFramework(FrameworkDatasetDescribeDataType value);

} // DataInputsType
