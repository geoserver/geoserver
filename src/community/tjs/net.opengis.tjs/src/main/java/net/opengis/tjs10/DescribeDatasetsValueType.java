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
 * A representation of the literals of the enumeration '<em><b>Describe Datasets Value Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='DescribeDatasetsValueType'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDescribeDatasetsValueType()
 */
public final class DescribeDatasetsValueType extends AbstractEnumerator {
    /**
     * The '<em><b>Describe Datasets</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>Describe Datasets</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @model name="DescribeDatasets"
     * @generated
     * @ordered
     * @see #DESCRIBE_DATASETS_LITERAL
     */
    public static final int DESCRIBE_DATASETS = 0;

    /**
     * The '<em><b>Describe Datasets</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #DESCRIBE_DATASETS
     */
    public static final DescribeDatasetsValueType DESCRIBE_DATASETS_LITERAL = new DescribeDatasetsValueType(DESCRIBE_DATASETS, "DescribeDatasets", "DescribeDatasets");

    /**
     * An array of all the '<em><b>Describe Datasets Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final DescribeDatasetsValueType[] VALUES_ARRAY =
            new DescribeDatasetsValueType[]{
                                                   DESCRIBE_DATASETS_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Describe Datasets Value Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Describe Datasets Value Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeDatasetsValueType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeDatasetsValueType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Datasets Value Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeDatasetsValueType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            DescribeDatasetsValueType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Describe Datasets Value Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static DescribeDatasetsValueType get(int value) {
        switch (value) {
            case DESCRIBE_DATASETS:
                return DESCRIBE_DATASETS_LITERAL;
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
    private DescribeDatasetsValueType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //DescribeDatasetsValueType
