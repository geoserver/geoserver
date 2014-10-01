/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.common.util.AbstractEnumerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Gaussian Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='gaussian_._type'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getGaussianType()
 */
public final class GaussianType extends AbstractEnumerator {
    /**
     * The '<em><b>True</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>True</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="true"
     * @generated
     * @ordered
     * @see #TRUE_LITERAL
     */
    public static final int TRUE = 0;

    /**
     * The '<em><b>False</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>False</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="false"
     * @generated
     * @ordered
     * @see #FALSE_LITERAL
     */
    public static final int FALSE = 1;

    /**
     * The '<em><b>Unknown</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Unknown</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="unknown"
     * @generated
     * @ordered
     * @see #UNKNOWN_LITERAL
     */
    public static final int UNKNOWN = 2;

    /**
     * The '<em><b>True</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #TRUE
     */
    public static final GaussianType TRUE_LITERAL = new GaussianType(TRUE, "true", "true");

    /**
     * The '<em><b>False</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #FALSE
     */
    public static final GaussianType FALSE_LITERAL = new GaussianType(FALSE, "false", "false");

    /**
     * The '<em><b>Unknown</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #UNKNOWN
     */
    public static final GaussianType UNKNOWN_LITERAL = new GaussianType(UNKNOWN, "unknown", "unknown");

    /**
     * An array of all the '<em><b>Gaussian Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final GaussianType[] VALUES_ARRAY =
            new GaussianType[]{
                                      TRUE_LITERAL,
                                      FALSE_LITERAL,
                                      UNKNOWN_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Gaussian Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Gaussian Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GaussianType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            GaussianType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Gaussian Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GaussianType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            GaussianType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Gaussian Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GaussianType get(int value) {
        switch (value) {
            case TRUE:
                return TRUE_LITERAL;
            case FALSE:
                return FALSE_LITERAL;
            case UNKNOWN:
                return UNKNOWN_LITERAL;
        }
        return null;
    }

    /**
     * Only this class can construct instances.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private GaussianType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //GaussianType
