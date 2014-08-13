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
 * A representation of the literals of the enumeration '<em><b>Describe Data Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='DescribeDataValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDescribeDataValueType()
 */
public final class DescribeDataValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Describe Data</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Describe Data</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="DescribeData"
     * @generated
     * @ordered
     * @see #DESCRIBE_DATA_LITERAL
     */
    public static final int DESCRIBE_DATA = 0;

    /**
     * The '<em><b>Describe Data</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #DESCRIBE_DATA
     */
    public static final DescribeDataValueType DESCRIBE_DATA_LITERAL = new DescribeDataValueType(DESCRIBE_DATA, "DescribeData", "DescribeData");

    /**
     * An array of all the '<em><b>Describe Data Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final DescribeDataValueType[] VALUES_ARRAY =
            new DescribeDataValueType[]{
                                               DESCRIBE_DATA_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Describe Data Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Describe Data Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeDataValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeDataValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Data Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeDataValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeDataValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Data Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeDataValueType get(int value) {
        switch (value) {
            case DESCRIBE_DATA:
                return DESCRIBE_DATA_LITERAL;
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
    private DescribeDataValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //DescribeDataValueType
