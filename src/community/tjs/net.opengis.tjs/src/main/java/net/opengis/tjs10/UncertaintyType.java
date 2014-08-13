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
 * A representation of the model object '<em><b>Uncertainty Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.UncertaintyType#getValue <em>Value</em>}</li>
 * <li>{@link net.opengis.tjs10.UncertaintyType#getGaussian <em>Gaussian</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Uncertainty_._type' kind='simple'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getUncertaintyType()
 */
public interface UncertaintyType extends EObject {
    /**
     * Returns the value of the '<em><b>Value</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Value</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Value</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="name=':0' kind='simple'"
     * @generated
     * @see #setValue(String)
     * @see net.opengis.tjs10.Tjs10Package#getUncertaintyType_Value()
     */
    String getValue();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.UncertaintyType#getValue <em>Value</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Value</em>' attribute.
     * @generated
     * @see #getValue()
     */
    void setValue(String value);

    /**
     * Returns the value of the '<em><b>Gaussian</b></em>' attribute.
     * The literals are from the enumeration {@link net.opengis.tjs10.GaussianType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Uncertainty is of a Gaussian form, and Independent and Identically Distributed (IID).
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Gaussian</em>' attribute.
     * @model unsettable="true" required="true"
     * extendedMetaData="kind='attribute' name='gaussian'"
     * @generated
     * @see net.opengis.tjs10.GaussianType
     * @see #isSetGaussian()
     * @see #unsetGaussian()
     * @see #setGaussian(GaussianType)
     * @see net.opengis.tjs10.Tjs10Package#getUncertaintyType_Gaussian()
     */
    GaussianType getGaussian();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.UncertaintyType#getGaussian <em>Gaussian</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Gaussian</em>' attribute.
     * @generated
     * @see net.opengis.tjs10.GaussianType
     * @see #isSetGaussian()
     * @see #unsetGaussian()
     * @see #getGaussian()
     */
    void setGaussian(GaussianType value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.UncertaintyType#getGaussian <em>Gaussian</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetGaussian()
     * @see #getGaussian()
     * @see #setGaussian(GaussianType)
     */
    void unsetGaussian();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.UncertaintyType#getGaussian <em>Gaussian</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Gaussian</em>' attribute is set.
     * @generated
     * @see #unsetGaussian()
     * @see #getGaussian()
     * @see #setGaussian(GaussianType)
     */
    boolean isSetGaussian();

} // UncertaintyType
