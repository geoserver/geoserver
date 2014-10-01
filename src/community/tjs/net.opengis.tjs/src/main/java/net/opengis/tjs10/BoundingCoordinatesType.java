/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.EObject;

import java.math.BigDecimal;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Bounding Coordinates Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.BoundingCoordinatesType#getNorth <em>North</em>}</li>
 * <li>{@link net.opengis.tjs10.BoundingCoordinatesType#getSouth <em>South</em>}</li>
 * <li>{@link net.opengis.tjs10.BoundingCoordinatesType#getEast <em>East</em>}</li>
 * <li>{@link net.opengis.tjs10.BoundingCoordinatesType#getWest <em>West</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='BoundingCoordinates_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getBoundingCoordinatesType()
 */
public interface BoundingCoordinatesType extends EObject {
    /**
     * Returns the value of the '<em><b>North</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * WGS84 latitude of the northernmost coordinate of the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>North</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.Decimal" required="true"
     * extendedMetaData="kind='element' name='North' namespace='##targetNamespace'"
     * @generated
     * @see #setNorth(BigDecimal)
     * @see net.opengis.tjs10.Tjs10Package#getBoundingCoordinatesType_North()
     */
    BigDecimal getNorth();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.BoundingCoordinatesType#getNorth <em>North</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>North</em>' attribute.
     * @generated
     * @see #getNorth()
     */
    void setNorth(BigDecimal value);

    /**
     * Returns the value of the '<em><b>South</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * WGS84 latitude of the southernmost coordinate of the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>South</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.Decimal" required="true"
     * extendedMetaData="kind='element' name='South' namespace='##targetNamespace'"
     * @generated
     * @see #setSouth(BigDecimal)
     * @see net.opengis.tjs10.Tjs10Package#getBoundingCoordinatesType_South()
     */
    BigDecimal getSouth();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.BoundingCoordinatesType#getSouth <em>South</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>South</em>' attribute.
     * @generated
     * @see #getSouth()
     */
    void setSouth(BigDecimal value);

    /**
     * Returns the value of the '<em><b>East</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * WGS84 longitude of the easternmost coordinate of the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>East</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.Decimal" required="true"
     * extendedMetaData="kind='element' name='East' namespace='##targetNamespace'"
     * @generated
     * @see #setEast(BigDecimal)
     * @see net.opengis.tjs10.Tjs10Package#getBoundingCoordinatesType_East()
     */
    BigDecimal getEast();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.BoundingCoordinatesType#getEast <em>East</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>East</em>' attribute.
     * @generated
     * @see #getEast()
     */
    void setEast(BigDecimal value);

    /**
     * Returns the value of the '<em><b>West</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * WGS84 longitude of the westernmost coordinate of the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>West</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.Decimal" required="true"
     * extendedMetaData="kind='element' name='West' namespace='##targetNamespace'"
     * @generated
     * @see #setWest(BigDecimal)
     * @see net.opengis.tjs10.Tjs10Package#getBoundingCoordinatesType_West()
     */
    BigDecimal getWest();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.BoundingCoordinatesType#getWest <em>West</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>West</em>' attribute.
     * @generated
     * @see #getWest()
     */
    void setWest(BigDecimal value);

} // BoundingCoordinatesType
