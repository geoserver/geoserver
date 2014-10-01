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
 * A representation of the literals of the enumeration '<em><b>Version Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='VersionType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getVersionType()
 */
public final class VersionType extends AbstractEnumerator {
    /**
     * The '<em><b>10</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>10</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model literal="1.0"
     * @generated
     * @ordered
     * @see #_10_LITERAL
     */
    public static final int _10 = 0;

    /**
     * The '<em><b>10</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #_10
     */
    public static final VersionType _10_LITERAL = new VersionType(_10, "_10", "1.0");

    /**
     * An array of all the '<em><b>Version Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final VersionType[] VALUES_ARRAY =
            new VersionType[]{
                                     _10_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Version Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Version Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static VersionType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            VersionType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Version Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static VersionType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            VersionType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Version Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static VersionType get(int value) {
        switch (value) {
            case _10:
                return _10_LITERAL;
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
    private VersionType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //VersionType
