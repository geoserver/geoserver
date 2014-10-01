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
 * A representation of the literals of the enumeration '<em><b>Describe Key Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='DescribeKeyValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDescribeKeyValueType()
 */
public final class DescribeKeyValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Describe Key</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Describe Key</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="DescribeKey"
     * @generated
     * @ordered
     * @see #DESCRIBE_KEY_LITERAL
     */
    public static final int DESCRIBE_KEY = 0;

    /**
     * The '<em><b>Describe Key</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #DESCRIBE_KEY
     */
    public static final DescribeKeyValueType DESCRIBE_KEY_LITERAL = new DescribeKeyValueType(DESCRIBE_KEY, "DescribeKey", "DescribeKey");

    /**
     * An array of all the '<em><b>Describe Key Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final DescribeKeyValueType[] VALUES_ARRAY =
            new DescribeKeyValueType[]{
                                              DESCRIBE_KEY_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Describe Key Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Describe Key Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeKeyValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeKeyValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Key Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeKeyValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeKeyValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Key Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeKeyValueType get(int value) {
        switch (value) {
            case DESCRIBE_KEY:
                return DESCRIBE_KEY_LITERAL;
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
    private DescribeKeyValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //DescribeKeyValueType
