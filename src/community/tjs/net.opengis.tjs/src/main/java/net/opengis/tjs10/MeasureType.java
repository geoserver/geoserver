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
 * A representation of the model object '<em><b>Measure Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.MeasureType#getUOM <em>UOM</em>}</li>
 * <li>{@link net.opengis.tjs10.MeasureType#getUncertainty <em>Uncertainty</em>}</li>
 * <li>{@link net.opengis.tjs10.MeasureType#getExceptions <em>Exceptions</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Measure_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getMeasureType()
 */
public interface MeasureType extends EObject {
    /**
     * Returns the value of the '<em><b>UOM</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Unit of Measure
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>UOM</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='UOM' namespace='##targetNamespace'"
     * @generated
     * @see #setUOM(UOMType)
     * @see net.opengis.tjs10.Tjs10Package#getMeasureType_UOM()
     */
    UOMType getUOM();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MeasureType#getUOM <em>UOM</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>UOM</em>' containment reference.
     * @generated
     * @see #getUOM()
     */
    void setUOM(UOMType value);

    /**
     * Returns the value of the '<em><b>Uncertainty</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Standard Uncertainty, according to the generally agreed upon definition described at sites like http://physics.nist.gov/cuu/Uncertainty/index.html
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Uncertainty</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Uncertainty' namespace='##targetNamespace'"
     * @generated
     * @see #setUncertainty(UncertaintyType)
     * @see net.opengis.tjs10.Tjs10Package#getMeasureType_Uncertainty()
     */
    UncertaintyType getUncertainty();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MeasureType#getUncertainty <em>Uncertainty</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Uncertainty</em>' containment reference.
     * @generated
     * @see #getUncertainty()
     */
    void setUncertainty(UncertaintyType value);

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
     * @see #setExceptions(MeasureCountExceptions)
     * @see net.opengis.tjs10.Tjs10Package#getMeasureType_Exceptions()
     */
    MeasureCountExceptions getExceptions();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MeasureType#getExceptions <em>Exceptions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Exceptions</em>' containment reference.
     * @generated
     * @see #getExceptions()
     */
    void setExceptions(MeasureCountExceptions value);

} // MeasureType
