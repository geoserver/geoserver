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
 * A representation of the literals of the enumeration '<em><b>Data Class Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='DataClass_._type'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDataClassType()
 */
public final class DataClassType extends AbstractEnumerator {
    /**
     * The '<em><b>Nominal</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Nominal</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="nominal"
     * @generated
     * @ordered
     * @see #NOMINAL_LITERAL
     */
    public static final int NOMINAL = 0;

    /**
     * The '<em><b>Ordinal</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Ordinal</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="ordinal"
     * @generated
     * @ordered
     * @see #ORDINAL_LITERAL
     */
    public static final int ORDINAL = 1;

    /**
     * The '<em><b>Measure</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Measure</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="measure"
     * @generated
     * @ordered
     * @see #MEASURE_LITERAL
     */
    public static final int MEASURE = 2;

    /**
     * The '<em><b>Count</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Count</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="count"
     * @generated
     * @ordered
     * @see #COUNT_LITERAL
     */
    public static final int COUNT = 3;

    /**
     * The '<em><b>Nominal</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #NOMINAL
     */
    public static final DataClassType NOMINAL_LITERAL = new DataClassType(NOMINAL, "nominal", "nominal");

    /**
     * The '<em><b>Ordinal</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #ORDINAL
     */
    public static final DataClassType ORDINAL_LITERAL = new DataClassType(ORDINAL, "ordinal", "ordinal");

    /**
     * The '<em><b>Measure</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #MEASURE
     */
    public static final DataClassType MEASURE_LITERAL = new DataClassType(MEASURE, "measure", "measure");

    /**
     * The '<em><b>Count</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #COUNT
     */
    public static final DataClassType COUNT_LITERAL = new DataClassType(COUNT, "count", "count");

    /**
     * An array of all the '<em><b>Data Class Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final DataClassType[] VALUES_ARRAY =
            new DataClassType[]{
                                       NOMINAL_LITERAL,
                                       ORDINAL_LITERAL,
                                       MEASURE_LITERAL,
                                       COUNT_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Data Class Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Data Class Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DataClassType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DataClassType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Data Class Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DataClassType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DataClassType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Data Class Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DataClassType get(int value) {
        switch (value) {
            case NOMINAL:
                return NOMINAL_LITERAL;
            case ORDINAL:
                return ORDINAL_LITERAL;
            case MEASURE:
                return MEASURE_LITERAL;
            case COUNT:
                return COUNT_LITERAL;
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
    private DataClassType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //DataClassType
