/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.BoundingCoordinatesType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import java.math.BigDecimal;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Bounding Coordinates Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.BoundingCoordinatesTypeImpl#getNorth <em>North</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.BoundingCoordinatesTypeImpl#getSouth <em>South</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.BoundingCoordinatesTypeImpl#getEast <em>East</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.BoundingCoordinatesTypeImpl#getWest <em>West</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class BoundingCoordinatesTypeImpl extends EObjectImpl implements BoundingCoordinatesType {
    /**
     * The default value of the '{@link #getNorth() <em>North</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getNorth()
     */
    protected static final BigDecimal NORTH_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getNorth() <em>North</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getNorth()
     */
    protected BigDecimal north = NORTH_EDEFAULT;

    /**
     * The default value of the '{@link #getSouth() <em>South</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getSouth()
     */
    protected static final BigDecimal SOUTH_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getSouth() <em>South</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getSouth()
     */
    protected BigDecimal south = SOUTH_EDEFAULT;

    /**
     * The default value of the '{@link #getEast() <em>East</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getEast()
     */
    protected static final BigDecimal EAST_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getEast() <em>East</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getEast()
     */
    protected BigDecimal east = EAST_EDEFAULT;

    /**
     * The default value of the '{@link #getWest() <em>West</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getWest()
     */
    protected static final BigDecimal WEST_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getWest() <em>West</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getWest()
     */
    protected BigDecimal west = WEST_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected BoundingCoordinatesTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getBoundingCoordinatesType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigDecimal getNorth() {
        return north;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setNorth(BigDecimal newNorth) {
        BigDecimal oldNorth = north;
        north = newNorth;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.BOUNDING_COORDINATES_TYPE__NORTH, oldNorth, north));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigDecimal getSouth() {
        return south;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setSouth(BigDecimal newSouth) {
        BigDecimal oldSouth = south;
        south = newSouth;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.BOUNDING_COORDINATES_TYPE__SOUTH, oldSouth, south));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigDecimal getEast() {
        return east;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setEast(BigDecimal newEast) {
        BigDecimal oldEast = east;
        east = newEast;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.BOUNDING_COORDINATES_TYPE__EAST, oldEast, east));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigDecimal getWest() {
        return west;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setWest(BigDecimal newWest) {
        BigDecimal oldWest = west;
        west = newWest;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.BOUNDING_COORDINATES_TYPE__WEST, oldWest, west));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__NORTH:
                return getNorth();
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__SOUTH:
                return getSouth();
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__EAST:
                return getEast();
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__WEST:
                return getWest();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__NORTH:
                setNorth((BigDecimal) newValue);
                return;
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__SOUTH:
                setSouth((BigDecimal) newValue);
                return;
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__EAST:
                setEast((BigDecimal) newValue);
                return;
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__WEST:
                setWest((BigDecimal) newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void eUnset(int featureID) {
        switch (featureID) {
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__NORTH:
                setNorth(NORTH_EDEFAULT);
                return;
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__SOUTH:
                setSouth(SOUTH_EDEFAULT);
                return;
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__EAST:
                setEast(EAST_EDEFAULT);
                return;
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__WEST:
                setWest(WEST_EDEFAULT);
                return;
        }
        super.eUnset(featureID);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean eIsSet(int featureID) {
        switch (featureID) {
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__NORTH:
                return NORTH_EDEFAULT == null ? north != null : !NORTH_EDEFAULT.equals(north);
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__SOUTH:
                return SOUTH_EDEFAULT == null ? south != null : !SOUTH_EDEFAULT.equals(south);
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__EAST:
                return EAST_EDEFAULT == null ? east != null : !EAST_EDEFAULT.equals(east);
            case Tjs10Package.BOUNDING_COORDINATES_TYPE__WEST:
                return WEST_EDEFAULT == null ? west != null : !WEST_EDEFAULT.equals(west);
        }
        return super.eIsSet(featureID);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String toString() {
        if (eIsProxy()) return super.toString();

        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (north: ");
        result.append(north);
        result.append(", south: ");
        result.append(south);
        result.append(", east: ");
        result.append(east);
        result.append(", west: ");
        result.append(west);
        result.append(')');
        return result.toString();
    }

} //BoundingCoordinatesTypeImpl
