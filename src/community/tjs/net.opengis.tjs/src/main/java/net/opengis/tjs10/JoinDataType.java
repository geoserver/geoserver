/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Join Data Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.JoinDataType#getAttributeData <em>Attribute Data</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataType#getMapStyling <em>Map Styling</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataType#getClassificationURL <em>Classification URL</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataType#getUpdate <em>Update</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='JoinData_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getJoinDataType()
 */
public interface JoinDataType extends RequestBaseType {
    /**
     * Returns the value of the '<em><b>Attribute Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Attribute data to be joined to the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Attribute Data</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='AttributeData' namespace='##targetNamespace'"
     * @generated
     * @see #setAttributeData(AttributeDataType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataType_AttributeData()
     */
    AttributeDataType getAttributeData();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataType#getAttributeData <em>Attribute Data</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Attribute Data</em>' containment reference.
     * @generated
     * @see #getAttributeData()
     */
    void setAttributeData(AttributeDataType value);

    /**
     * Returns the value of the '<em><b>Map Styling</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Styling that shall be applied if the AccessMechanisms of the requested output includes WMS.  If WMS is not supported, this element shall not be present.  If WMS is supported and this element is not present, a default styling will be applied to the WMS layer.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Map Styling</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='MapStyling' namespace='##targetNamespace'"
     * @generated
     * @see #setMapStyling(MapStylingType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataType_MapStyling()
     */
    MapStylingType getMapStyling();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataType#getMapStyling <em>Map Styling</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Map Styling</em>' containment reference.
     * @generated
     * @see #getMapStyling()
     */
    void setMapStyling(MapStylingType value);

    /**
     * Returns the value of the '<em><b>Classification URL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL that returns a file describing a data classification to be applied to the output (e.g. the classification to be used for a legend in the case where the output is a WMS). This file must be encoded in compliance with the XML Schema identified in the ClassificationSchemaURL element of the DescribeJoinAbilities response.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Classification URL</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='ClassificationURL' namespace='##targetNamespace'"
     * @generated
     * @see #setClassificationURL(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataType_ClassificationURL()
     */
    EObject getClassificationURL();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataType#getClassificationURL <em>Classification URL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Classification URL</em>' containment reference.
     * @generated
     * @see #getClassificationURL()
     */
    void setClassificationURL(EObject value);

    /**
     * Returns the value of the '<em><b>Update</b></em>' attribute.
     * The literals are from the enumeration {@link net.opengis.tjs10.UpdateType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Flag to indicate if the Rowset content would be used to update/replace any equivalent attribute data that currently exists on the server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Update</em>' attribute.
     * @model unsettable="true"
     * extendedMetaData="kind='attribute' name='update'"
     * @generated
     * @see net.opengis.tjs10.UpdateType
     * @see #isSetUpdate()
     * @see #unsetUpdate()
     * @see #setUpdate(UpdateType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataType_Update()
     */
    UpdateType getUpdate();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataType#getUpdate <em>Update</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Update</em>' attribute.
     * @generated
     * @see net.opengis.tjs10.UpdateType
     * @see #isSetUpdate()
     * @see #unsetUpdate()
     * @see #getUpdate()
     */
    void setUpdate(UpdateType value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.JoinDataType#getUpdate <em>Update</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetUpdate()
     * @see #getUpdate()
     * @see #setUpdate(UpdateType)
     */
    void unsetUpdate();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.JoinDataType#getUpdate <em>Update</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Update</em>' attribute is set.
     * @generated
     * @see #unsetUpdate()
     * @see #getUpdate()
     * @see #setUpdate(UpdateType)
     */
    boolean isSetUpdate();

} // JoinDataType
