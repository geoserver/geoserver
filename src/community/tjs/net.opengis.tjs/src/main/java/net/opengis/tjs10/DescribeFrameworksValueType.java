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
 * A representation of the literals of the enumeration '<em><b>Describe Frameworks Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='DescribeFrameworksValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDescribeFrameworksValueType()
 */
public final class DescribeFrameworksValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Describe Frameworks</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Describe Frameworks</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="DescribeFrameworks"
     * @generated
     * @ordered
     * @see #DESCRIBE_FRAMEWORKS_LITERAL
     */
    public static final int DESCRIBE_FRAMEWORKS = 0;

    /**
     * The '<em><b>Describe Frameworks</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #DESCRIBE_FRAMEWORKS
     */
    public static final DescribeFrameworksValueType DESCRIBE_FRAMEWORKS_LITERAL = new DescribeFrameworksValueType(DESCRIBE_FRAMEWORKS, "DescribeFrameworks", "DescribeFrameworks");

    /**
     * An array of all the '<em><b>Describe Frameworks Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final DescribeFrameworksValueType[] VALUES_ARRAY =
            new DescribeFrameworksValueType[]{
                                                     DESCRIBE_FRAMEWORKS_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Describe Frameworks Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Describe Frameworks Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeFrameworksValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeFrameworksValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Frameworks Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeFrameworksValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeFrameworksValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Frameworks Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeFrameworksValueType get(int value) {
        switch (value) {
            case DESCRIBE_FRAMEWORKS:
                return DESCRIBE_FRAMEWORKS_LITERAL;
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
    private DescribeFrameworksValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //DescribeFrameworksValueType
