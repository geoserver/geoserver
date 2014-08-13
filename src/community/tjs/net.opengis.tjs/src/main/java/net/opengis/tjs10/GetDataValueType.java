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
 * A representation of the literals of the enumeration '<em><b>Get Data Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='GetDataValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getGetDataValueType()
 */
public final class GetDataValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Get Data</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Get Data</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="GetData"
     * @generated
     * @ordered
     * @see #GET_DATA_LITERAL
     */
    public static final int GET_DATA = 0;

    /**
     * The '<em><b>Get Data</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #GET_DATA
     */
    public static final GetDataValueType GET_DATA_LITERAL = new GetDataValueType(GET_DATA, "GetData", "GetData");

    /**
     * An array of all the '<em><b>Get Data Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final GetDataValueType[] VALUES_ARRAY =
            new GetDataValueType[]{
                                          GET_DATA_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Get Data Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Get Data Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GetDataValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            GetDataValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Get Data Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GetDataValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            GetDataValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Get Data Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GetDataValueType get(int value) {
        switch (value) {
            case GET_DATA:
                return GET_DATA_LITERAL;
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
    private GetDataValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //GetDataValueType
