/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.EObject;

import java.math.BigInteger;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Column Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ColumnType#getDecimals <em>Decimals</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType#getLength <em>Length</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType#getName <em>Name</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Column_._type' kind='empty'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getColumnType()
 */
public interface ColumnType extends EObject {
    /**
     * Returns the value of the '<em><b>Decimals</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Number of digits after the decimal, for decimal numbers with a fixed number of digits after the decimal.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Decimals</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.NonNegativeInteger"
     * extendedMetaData="kind='attribute' name='decimals'"
     * @generated
     * @see #setDecimals(BigInteger)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType_Decimals()
     */
    BigInteger getDecimals();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType#getDecimals <em>Decimals</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Decimals</em>' attribute.
     * @generated
     * @see #getDecimals()
     */
    void setDecimals(BigInteger value);

    /**
     * Returns the value of the '<em><b>Length</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Length of the field, in characters.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Length</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.NonNegativeInteger" required="true"
     * extendedMetaData="kind='attribute' name='length'"
     * @generated
     * @see #setLength(BigInteger)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType_Length()
     */
    BigInteger getLength();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType#getLength <em>Length</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Length</em>' attribute.
     * @generated
     * @see #getLength()
     */
    void setLength(BigInteger value);

    /**
     * Returns the value of the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Name of the key field in the spatial framework dataset through which data can be joined.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Name</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='attribute' name='name'"
     * @generated
     * @see #setName(String)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType_Name()
     */
    String getName();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType#getName <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Name</em>' attribute.
     * @generated
     * @see #getName()
     */
    void setName(String value);

    /**
     * Returns the value of the '<em><b>Type</b></em>' attribute.
     * The literals are from the enumeration {@link net.opengis.tjs10.TypeType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Datatype, as defined by XML schema at http://www.w3.org/TR/xmlschema-2/#.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Type</em>' attribute.
     * @model unsettable="true" required="true"
     * extendedMetaData="kind='attribute' name='type'"
     * @generated
     * @see net.opengis.tjs10.TypeType
     * @see #isSetType()
     * @see #unsetType()
     * @see #setType(TypeType)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType_Type()
     */
    TypeType getType();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType#getType <em>Type</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Type</em>' attribute.
     * @generated
     * @see net.opengis.tjs10.TypeType
     * @see #isSetType()
     * @see #unsetType()
     * @see #getType()
     */
    void setType(TypeType value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.ColumnType#getType <em>Type</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetType()
     * @see #getType()
     * @see #setType(TypeType)
     */
    void unsetType();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.ColumnType#getType <em>Type</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Type</em>' attribute is set.
     * @generated
     * @see #unsetType()
     * @see #getType()
     * @see #setType(TypeType)
     */
    boolean isSetType();

} // ColumnType
