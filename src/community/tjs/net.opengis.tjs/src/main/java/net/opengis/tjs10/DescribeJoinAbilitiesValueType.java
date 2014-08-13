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
 * A representation of the literals of the enumeration '<em><b>Describe Join Abilities Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='DescribeJoinAbilitiesValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDescribeJoinAbilitiesValueType()
 */
public final class DescribeJoinAbilitiesValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Describe Join Abilities</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Describe Join Abilities</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="DescribeJoinAbilities"
     * @generated
     * @ordered
     * @see #DESCRIBE_JOIN_ABILITIES_LITERAL
     */
    public static final int DESCRIBE_JOIN_ABILITIES = 0;

    /**
     * The '<em><b>Describe Join Abilities</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #DESCRIBE_JOIN_ABILITIES
     */
    public static final DescribeJoinAbilitiesValueType DESCRIBE_JOIN_ABILITIES_LITERAL = new DescribeJoinAbilitiesValueType(DESCRIBE_JOIN_ABILITIES, "DescribeJoinAbilities", "DescribeJoinAbilities");

    /**
     * An array of all the '<em><b>Describe Join Abilities Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final DescribeJoinAbilitiesValueType[] VALUES_ARRAY =
            new DescribeJoinAbilitiesValueType[]{
                                                        DESCRIBE_JOIN_ABILITIES_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Describe Join Abilities Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Describe Join Abilities Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeJoinAbilitiesValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeJoinAbilitiesValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Join Abilities Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeJoinAbilitiesValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeJoinAbilitiesValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Join Abilities Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeJoinAbilitiesValueType get(int value) {
        switch (value) {
            case DESCRIBE_JOIN_ABILITIES:
                return DESCRIBE_JOIN_ABILITIES_LITERAL;
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
    private DescribeJoinAbilitiesValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //DescribeJoinAbilitiesValueType
