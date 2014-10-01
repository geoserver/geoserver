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
 * A representation of the model object '<em><b>Values Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ValuesType#getNominal <em>Nominal</em>}</li>
 * <li>{@link net.opengis.tjs10.ValuesType#getOrdinal <em>Ordinal</em>}</li>
 * <li>{@link net.opengis.tjs10.ValuesType#getCount <em>Count</em>}</li>
 * <li>{@link net.opengis.tjs10.ValuesType#getMeasure <em>Measure</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Values_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getValuesType()
 */
public interface ValuesType extends EObject {
    /**
     * Returns the value of the '<em><b>Nominal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Data consists of unique names for spatial features
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Nominal</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Nominal' namespace='##targetNamespace'"
     * @generated
     * @see #setNominal(NominalType)
     * @see net.opengis.tjs10.Tjs10Package#getValuesType_Nominal()
     */
    NominalType getNominal();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ValuesType#getNominal <em>Nominal</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Nominal</em>' containment reference.
     * @generated
     * @see #getNominal()
     */
    void setNominal(NominalType value);

    /**
     * Returns the value of the '<em><b>Ordinal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Data consists of a ranked (ordered) classification
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Ordinal</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Ordinal' namespace='##targetNamespace'"
     * @generated
     * @see #setOrdinal(OrdinalType)
     * @see net.opengis.tjs10.Tjs10Package#getValuesType_Ordinal()
     */
    OrdinalType getOrdinal();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ValuesType#getOrdinal <em>Ordinal</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Ordinal</em>' containment reference.
     * @generated
     * @see #getOrdinal()
     */
    void setOrdinal(OrdinalType value);

    /**
     * Returns the value of the '<em><b>Count</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Data consists of the number of some observable elements present in the spatial features
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Count</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Count' namespace='##targetNamespace'"
     * @generated
     * @see #setCount(CountType)
     * @see net.opengis.tjs10.Tjs10Package#getValuesType_Count()
     */
    CountType getCount();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ValuesType#getCount <em>Count</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Count</em>' containment reference.
     * @generated
     * @see #getCount()
     */
    void setCount(CountType value);

    /**
     * Returns the value of the '<em><b>Measure</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Data consists of measurements of some characteristic attributable to the spatial features
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Measure</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Measure' namespace='##targetNamespace'"
     * @generated
     * @see #setMeasure(MeasureType)
     * @see net.opengis.tjs10.Tjs10Package#getValuesType_Measure()
     */
    MeasureType getMeasure();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ValuesType#getMeasure <em>Measure</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Measure</em>' containment reference.
     * @generated
     * @see #getMeasure()
     */
    void setMeasure(MeasureType value);

} // ValuesType
