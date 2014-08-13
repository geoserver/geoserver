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
 * A representation of the literals of the enumeration '<em><b>Purpose Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 *
 * @model extendedMetaData="name='purpose_._type'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getPurposeType()
 */
public final class PurposeType extends AbstractEnumerator {
    /**
     * The '<em><b>Spatial Component Identifier</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains an abstract nominal or ordinal identifier for spatial components found within the feature.  This value is for use when KeyRelationship = �many�.
     * <!-- end-model-doc -->
     *
     * @model name="SpatialComponentIdentifier"
     * @generated
     * @ordered
     * @see #SPATIAL_COMPONENT_IDENTIFIER_LITERAL
     */
    public static final int SPATIAL_COMPONENT_IDENTIFIER = 0;

    /**
     * The '<em><b>Spatial Component Proportion</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a proportion (from 0 to 1) of the spatial feature (i.e. the object identified by the PrimarySpatialIdentifier) to which the component applies.  For use when KeyRelationship = �many�.
     * <!-- end-model-doc -->
     *
     * @model name="SpatialComponentProportion"
     * @generated
     * @ordered
     * @see #SPATIAL_COMPONENT_PROPORTION_LITERAL
     */
    public static final int SPATIAL_COMPONENT_PROPORTION = 1;

    /**
     * The '<em><b>Spatial Component Percentage</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a percentage (from 0 to 100) of the spatial feature (i.e. the object identified by the PrimarySpatialIdentifier) to which the component applies.  For use when KeyRelationship = �many�.
     * <!-- end-model-doc -->
     *
     * @model name="SpatialComponentPercentage"
     * @generated
     * @ordered
     * @see #SPATIAL_COMPONENT_PERCENTAGE_LITERAL
     */
    public static final int SPATIAL_COMPONENT_PERCENTAGE = 2;

    /**
     * The '<em><b>Temporal Identifier</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a nominal or ordinal identifier that indicates the temporal positioning of the data (e.g. first).  For use when KeyRelationship = �many�.
     * <!-- end-model-doc -->
     *
     * @model name="TemporalIdentifier"
     * @generated
     * @ordered
     * @see #TEMPORAL_IDENTIFIER_LITERAL
     */
    public static final int TEMPORAL_IDENTIFIER = 3;

    /**
     * The '<em><b>Temporal Value</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a date/time measure (e.g. 2001).  For use when KeyRelationship = �many�.
     * <!-- end-model-doc -->
     *
     * @model name="TemporalValue"
     * @generated
     * @ordered
     * @see #TEMPORAL_VALUE_LITERAL
     */
    public static final int TEMPORAL_VALUE = 4;

    /**
     * The '<em><b>Vertical Identifier</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a nominal or ordinal identifier that indicates the depth or elevation of the data (e.g. lowest).  For use when KeyRelationship = �many�.
     * <!-- end-model-doc -->
     *
     * @model name="VerticalIdentifier"
     * @generated
     * @ordered
     * @see #VERTICAL_IDENTIFIER_LITERAL
     */
    public static final int VERTICAL_IDENTIFIER = 5;

    /**
     * The '<em><b>Vertical Value</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a numeric measure of the depth or elevation of the data (e.g. 120).  For use when KeyRelationship = �many�.
     * <!-- end-model-doc -->
     *
     * @model name="VerticalValue"
     * @generated
     * @ordered
     * @see #VERTICAL_VALUE_LITERAL
     */
    public static final int VERTICAL_VALUE = 6;

    /**
     * The '<em><b>Other Spatial Identifier</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a geographic linkage key for some other spatial Framework (i.e. not the one identified in the parent �Framework� element).  For use when other spatial identifiers are present in the tabular data, as may be the case for data which applies to a spatial hierarchy.
     * <!-- end-model-doc -->
     *
     * @model name="OtherSpatialIdentifier"
     * @generated
     * @ordered
     * @see #OTHER_SPATIAL_IDENTIFIER_LITERAL
     */
    public static final int OTHER_SPATIAL_IDENTIFIER = 7;

    /**
     * The '<em><b>Non Spatial Identifier</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains a nonspatial identifier - i.e. a relate key used to perform a table join to a table which does not contain spatial geometery.
     * <!-- end-model-doc -->
     *
     * @model name="NonSpatialIdentifier"
     * @generated
     * @ordered
     * @see #NON_SPATIAL_IDENTIFIER_LITERAL
     */
    public static final int NON_SPATIAL_IDENTIFIER = 8;

    /**
     * The '<em><b>Attribute</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Column contains attribute data describing a geographic object in the spatial Framework (i.e. suitable for mapping).
     * <!-- end-model-doc -->
     *
     * @model name="Attribute"
     * @generated
     * @ordered
     * @see #ATTRIBUTE_LITERAL
     */
    public static final int ATTRIBUTE = 9;

    /**
     * The '<em><b>Spatial Component Identifier</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #SPATIAL_COMPONENT_IDENTIFIER
     */
    public static final PurposeType SPATIAL_COMPONENT_IDENTIFIER_LITERAL = new PurposeType(SPATIAL_COMPONENT_IDENTIFIER, "SpatialComponentIdentifier", "SpatialComponentIdentifier");

    /**
     * The '<em><b>Spatial Component Proportion</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #SPATIAL_COMPONENT_PROPORTION
     */
    public static final PurposeType SPATIAL_COMPONENT_PROPORTION_LITERAL = new PurposeType(SPATIAL_COMPONENT_PROPORTION, "SpatialComponentProportion", "SpatialComponentProportion");

    /**
     * The '<em><b>Spatial Component Percentage</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #SPATIAL_COMPONENT_PERCENTAGE
     */
    public static final PurposeType SPATIAL_COMPONENT_PERCENTAGE_LITERAL = new PurposeType(SPATIAL_COMPONENT_PERCENTAGE, "SpatialComponentPercentage", "SpatialComponentPercentage");

    /**
     * The '<em><b>Temporal Identifier</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #TEMPORAL_IDENTIFIER
     */
    public static final PurposeType TEMPORAL_IDENTIFIER_LITERAL = new PurposeType(TEMPORAL_IDENTIFIER, "TemporalIdentifier", "TemporalIdentifier");

    /**
     * The '<em><b>Temporal Value</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #TEMPORAL_VALUE
     */
    public static final PurposeType TEMPORAL_VALUE_LITERAL = new PurposeType(TEMPORAL_VALUE, "TemporalValue", "TemporalValue");

    /**
     * The '<em><b>Vertical Identifier</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #VERTICAL_IDENTIFIER
     */
    public static final PurposeType VERTICAL_IDENTIFIER_LITERAL = new PurposeType(VERTICAL_IDENTIFIER, "VerticalIdentifier", "VerticalIdentifier");

    /**
     * The '<em><b>Vertical Value</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #VERTICAL_VALUE
     */
    public static final PurposeType VERTICAL_VALUE_LITERAL = new PurposeType(VERTICAL_VALUE, "VerticalValue", "VerticalValue");

    /**
     * The '<em><b>Other Spatial Identifier</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #OTHER_SPATIAL_IDENTIFIER
     */
    public static final PurposeType OTHER_SPATIAL_IDENTIFIER_LITERAL = new PurposeType(OTHER_SPATIAL_IDENTIFIER, "OtherSpatialIdentifier", "OtherSpatialIdentifier");

    /**
     * The '<em><b>Non Spatial Identifier</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #NON_SPATIAL_IDENTIFIER
     */
    public static final PurposeType NON_SPATIAL_IDENTIFIER_LITERAL = new PurposeType(NON_SPATIAL_IDENTIFIER, "NonSpatialIdentifier", "NonSpatialIdentifier");

    /**
     * The '<em><b>Attribute</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #ATTRIBUTE
     */
    public static final PurposeType ATTRIBUTE_LITERAL = new PurposeType(ATTRIBUTE, "Attribute", "Attribute");

    /**
     * An array of all the '<em><b>Purpose Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final PurposeType[] VALUES_ARRAY =
            new PurposeType[]{
                                     SPATIAL_COMPONENT_IDENTIFIER_LITERAL,
                                     SPATIAL_COMPONENT_PROPORTION_LITERAL,
                                     SPATIAL_COMPONENT_PERCENTAGE_LITERAL,
                                     TEMPORAL_IDENTIFIER_LITERAL,
                                     TEMPORAL_VALUE_LITERAL,
                                     VERTICAL_IDENTIFIER_LITERAL,
                                     VERTICAL_VALUE_LITERAL,
                                     OTHER_SPATIAL_IDENTIFIER_LITERAL,
                                     NON_SPATIAL_IDENTIFIER_LITERAL,
                                     ATTRIBUTE_LITERAL,
            };

    /**
     * A public read-only list of all the '<em><b>Purpose Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Purpose Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static PurposeType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            PurposeType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Purpose Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static PurposeType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            PurposeType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Purpose Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static PurposeType get(int value) {
        switch (value) {
            case SPATIAL_COMPONENT_IDENTIFIER:
                return SPATIAL_COMPONENT_IDENTIFIER_LITERAL;
            case SPATIAL_COMPONENT_PROPORTION:
                return SPATIAL_COMPONENT_PROPORTION_LITERAL;
            case SPATIAL_COMPONENT_PERCENTAGE:
                return SPATIAL_COMPONENT_PERCENTAGE_LITERAL;
            case TEMPORAL_IDENTIFIER:
                return TEMPORAL_IDENTIFIER_LITERAL;
            case TEMPORAL_VALUE:
                return TEMPORAL_VALUE_LITERAL;
            case VERTICAL_IDENTIFIER:
                return VERTICAL_IDENTIFIER_LITERAL;
            case VERTICAL_VALUE:
                return VERTICAL_VALUE_LITERAL;
            case OTHER_SPATIAL_IDENTIFIER:
                return OTHER_SPATIAL_IDENTIFIER_LITERAL;
            case NON_SPATIAL_IDENTIFIER:
                return NON_SPATIAL_IDENTIFIER_LITERAL;
            case ATTRIBUTE:
                return ATTRIBUTE_LITERAL;
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
    private PurposeType(int value, String name, String literal) {
        super(value, name, literal);
    }

} //PurposeType
