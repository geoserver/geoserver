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
 * A representation of the model object '<em><b>Nominal Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.NominalType#getClasses <em>Classes</em>}</li>
 * <li>{@link net.opengis.tjs10.NominalType#getExceptions <em>Exceptions</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Nominal_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getNominalType()
 */
public interface NominalType extends EObject {
    /**
     * Returns the value of the '<em><b>Classes</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Valid nominal classes for this attribute.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Classes</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Classes' namespace='##targetNamespace'"
     * @generated
     * @see #setClasses(ClassesType1)
     * @see net.opengis.tjs10.Tjs10Package#getNominalType_Classes()
     */
    ClassesType1 getClasses();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.NominalType#getClasses <em>Classes</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Classes</em>' containment reference.
     * @generated
     * @see #getClasses()
     */
    void setClasses(ClassesType1 value);

    /**
     * Returns the value of the '<em><b>Exceptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Valid exception classes for this attribute.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Exceptions</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Exceptions' namespace='##targetNamespace'"
     * @generated
     * @see #setExceptions(NominalOrdinalExceptions)
     * @see net.opengis.tjs10.Tjs10Package#getNominalType_Exceptions()
     */
    NominalOrdinalExceptions getExceptions();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.NominalType#getExceptions <em>Exceptions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Exceptions</em>' containment reference.
     * @generated
     * @see #getExceptions()
     */
    void setExceptions(NominalOrdinalExceptions value);

} // NominalType
