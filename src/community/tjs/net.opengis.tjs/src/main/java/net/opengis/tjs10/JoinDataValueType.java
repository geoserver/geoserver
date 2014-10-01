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
 * A representation of the literals of the enumeration '<em><b>Join Data Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='JoinDataValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getJoinDataValueType()
 */
public final class JoinDataValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Join Data</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Join Data</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="JoinData"
     * @generated
     * @ordered
     * @see #JOIN_DATA_LITERAL
     */
    public static final int JOIN_DATA = 0;

    /**
     * The '<em><b>Join Data</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #JOIN_DATA
     */
    public static final JoinDataValueType JOIN_DATA_LITERAL = new JoinDataValueType(JOIN_DATA, "JoinData", "JoinData");

    /**
     * An array of all the '<em><b>Join Data Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final JoinDataValueType[] VALUES_ARRAY =
            new JoinDataValueType[]{
                                           JOIN_DATA_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Join Data Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Join Data Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static JoinDataValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            JoinDataValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Join Data Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static JoinDataValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            JoinDataValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Join Data Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static JoinDataValueType get(int value) {
        switch (value) {
            case JOIN_DATA:
                return JOIN_DATA_LITERAL;
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
    private JoinDataValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //JoinDataValueType
