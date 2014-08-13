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
 * A representation of the model object '<em><b>Column Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ColumnType1#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getValues <em>Values</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getGetDataRequest <em>Get Data Request</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getDecimals <em>Decimals</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getLength <em>Length</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getName <em>Name</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getPurpose <em>Purpose</em>}</li>
 * <li>{@link net.opengis.tjs10.ColumnType1#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Column_._1_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getColumnType1()
 */
public interface ColumnType1 extends EObject {
    /**
     * Returns the value of the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Title</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Title' namespace='##targetNamespace'"
     * @generated
     * @see #setTitle(String)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Title()
     */
    String getTitle();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getTitle <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Title</em>' attribute.
     * @generated
     * @see #getTitle()
     */
    void setTitle(String value);

    /**
     * Returns the value of the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * One or more paragraphs of human-readable text describing the attribute and suitable for display in a pop-up window.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Abstract</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Abstract' namespace='##targetNamespace'"
     * @generated
     * @see #setAbstract(AbstractType)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Abstract()
     */
    AbstractType getAbstract();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getAbstract <em>Abstract</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Abstract</em>' containment reference.
     * @generated
     * @see #getAbstract()
     */
    void setAbstract(AbstractType value);

    /**
     * Returns the value of the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to a web-accessible resource which contains further information describing this object.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Documentation</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI"
     * extendedMetaData="kind='element' name='Documentation' namespace='##targetNamespace'"
     * @generated
     * @see #setDocumentation(String)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Documentation()
     */
    String getDocumentation();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getDocumentation <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Documentation</em>' attribute.
     * @generated
     * @see #getDocumentation()
     */
    void setDocumentation(String value);

    /**
     * Returns the value of the '<em><b>Values</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Type of values and valid values for the contents of this column.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Values</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Values' namespace='##targetNamespace'"
     * @generated
     * @see #setValues(ValuesType)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Values()
     */
    ValuesType getValues();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getValues <em>Values</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Values</em>' containment reference.
     * @generated
     * @see #getValues()
     */
    void setValues(ValuesType value);

    /**
     * Returns the value of the '<em><b>Get Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to the GetData request for this column.  The request shall include any other columns  that describe or quantify the values in this attribute column (i.e. where the "purpose" of the column is SpatialComponentIdentifier, TemporalIdentfier, etc).
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Get Data Request</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='GetDataRequest' namespace='##targetNamespace'"
     * @generated
     * @see #setGetDataRequest(GetDataRequestType)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_GetDataRequest()
     */
    GetDataRequestType getGetDataRequest();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getGetDataRequest <em>Get Data Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Get Data Request</em>' containment reference.
     * @generated
     * @see #getGetDataRequest()
     */
    void setGetDataRequest(GetDataRequestType value);

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
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Decimals()
     */
    BigInteger getDecimals();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getDecimals <em>Decimals</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Length()
     */
    BigInteger getLength();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getLength <em>Length</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Name()
     */
    String getName();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getName <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Name</em>' attribute.
     * @generated
     * @see #getName()
     */
    void setName(String value);

    /**
     * Returns the value of the '<em><b>Purpose</b></em>' attribute.
     * The literals are from the enumeration {@link net.opengis.tjs10.PurposeType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Primary purpose of the attribute, indicating whether the column contains attribute data or a linkage key to some other spatial framework or nonspatial data table.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Purpose</em>' attribute.
     * @model unsettable="true" required="true"
     * extendedMetaData="kind='attribute' name='purpose'"
     * @generated
     * @see net.opengis.tjs10.PurposeType
     * @see #isSetPurpose()
     * @see #unsetPurpose()
     * @see #setPurpose(PurposeType)
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Purpose()
     */
    PurposeType getPurpose();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getPurpose <em>Purpose</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Purpose</em>' attribute.
     * @generated
     * @see net.opengis.tjs10.PurposeType
     * @see #isSetPurpose()
     * @see #unsetPurpose()
     * @see #getPurpose()
     */
    void setPurpose(PurposeType value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.ColumnType1#getPurpose <em>Purpose</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetPurpose()
     * @see #getPurpose()
     * @see #setPurpose(PurposeType)
     */
    void unsetPurpose();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.ColumnType1#getPurpose <em>Purpose</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Purpose</em>' attribute is set.
     * @generated
     * @see #unsetPurpose()
     * @see #getPurpose()
     * @see #setPurpose(PurposeType)
     */
    boolean isSetPurpose();

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
     * @see net.opengis.tjs10.Tjs10Package#getColumnType1_Type()
     */
    TypeType getType();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ColumnType1#getType <em>Type</em>}' attribute.
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
     * Unsets the value of the '{@link net.opengis.tjs10.ColumnType1#getType <em>Type</em>}' attribute.
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
     * Returns whether the value of the '{@link net.opengis.tjs10.ColumnType1#getType <em>Type</em>}' attribute is set.
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

} // ColumnType1
