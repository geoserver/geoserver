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
 * A representation of the literals of the enumeration '<em><b>Version Type2</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='version_._type'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getVersionType2()
 */
public final class VersionType2 extends AbstractEnumerator {
    /**
     * The '<em><b>1</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>1</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model literal="1"
     * @generated
     * @ordered
     * @see #_1_LITERAL
     */
    public static final int _1 = 0;

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
    public static final int _10 = 1;

    /**
     * The '<em><b>100</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>100</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model literal="1.0.0"
     * @generated
     * @ordered
     * @see #_100_LITERAL
     */
    public static final int _100 = 2;

    /**
     * The '<em><b>1</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #_1
     */
    public static final VersionType2 _1_LITERAL = new VersionType2(_1, "_1", "1");

    /**
     * The '<em><b>10</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #_10
     */
    public static final VersionType2 _10_LITERAL = new VersionType2(_10, "_10", "1.0");

    /**
     * The '<em><b>100</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #_100
     */
    public static final VersionType2 _100_LITERAL = new VersionType2(_100, "_100", "1.0.0");

    /**
     * An array of all the '<em><b>Version Type2</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final VersionType2[] VALUES_ARRAY =
            new VersionType2[]{
                                      _1_LITERAL,
                                      _10_LITERAL,
                                      _100_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Version Type2</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Version Type2</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static VersionType2 get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            VersionType2 result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Version Type2</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static VersionType2 getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            VersionType2 result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Version Type2</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static VersionType2 get(int value) {
        switch (value) {
            case _1:
                return _1_LITERAL;
            case _10:
                return _10_LITERAL;
            case _100:
                return _100_LITERAL;
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
    private VersionType2(int value, String name, String literal) {
        super(value, name, literal);
    }

} //VersionType2
