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
 * A representation of the literals of the enumeration '<em><b>Get Capabilities Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='GetCapabilitiesValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesValueType()
 */
public final class GetCapabilitiesValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Get Capabilities</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Get Capabilities</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="GetCapabilities"
     * @generated
     * @ordered
     * @see #GET_CAPABILITIES_LITERAL
     */
    public static final int GET_CAPABILITIES = 0;

    /**
     * The '<em><b>Get Capabilities</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #GET_CAPABILITIES
     */
    public static final GetCapabilitiesValueType GET_CAPABILITIES_LITERAL = new GetCapabilitiesValueType(GET_CAPABILITIES, "GetCapabilities", "GetCapabilities");

    /**
     * An array of all the '<em><b>Get Capabilities Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final GetCapabilitiesValueType[] VALUES_ARRAY =
            new GetCapabilitiesValueType[]{
                                                  GET_CAPABILITIES_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Get Capabilities Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Get Capabilities Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GetCapabilitiesValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            GetCapabilitiesValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Get Capabilities Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GetCapabilitiesValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            GetCapabilitiesValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Get Capabilities Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static GetCapabilitiesValueType get(int value) {
        switch (value) {
            case GET_CAPABILITIES:
                return GET_CAPABILITIES_LITERAL;
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
    private GetCapabilitiesValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //GetCapabilitiesValueType
