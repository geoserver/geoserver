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
 * A representation of the literals of the enumeration '<em><b>Request Service Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='RequestServiceType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getRequestServiceType()
 */
public final class RequestServiceType extends AbstractEnumerator {
    /**
     * The '<em><b>TJS</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>TJS</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model
     * @generated
     * @ordered
     * @see #TJS_LITERAL
     */
    public static final int TJS = 0;

    /**
     * The '<em><b>TJS</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #TJS
     */
    public static final RequestServiceType TJS_LITERAL = new RequestServiceType(TJS, "TJS", "TJS");

    /**
     * An array of all the '<em><b>Request Service Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final RequestServiceType[] VALUES_ARRAY =
            new RequestServiceType[]{
                                            TJS_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Request Service Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Request Service Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static RequestServiceType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            RequestServiceType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Request Service Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static RequestServiceType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            RequestServiceType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Request Service Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static RequestServiceType get(int value) {
        switch (value) {
            case TJS:
                return TJS_LITERAL;
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
    private RequestServiceType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //RequestServiceType
