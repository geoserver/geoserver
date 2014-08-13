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
 * A representation of the model object '<em><b>Map Styling Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.MapStylingType#getStylingIdentifier <em>Styling Identifier</em>}</li>
 * <li>{@link net.opengis.tjs10.MapStylingType#getStylingURL <em>Styling URL</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='MapStyling_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getMapStylingType()
 */
public interface MapStylingType extends EObject {
    /**
     * Returns the value of the '<em><b>Styling Identifier</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Name that identifies the type of styling to be invoked.  Must be a styling Identifier listed in the DescribeJoinAbilities response.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Styling Identifier</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='StylingIdentifier' namespace='##targetNamespace'"
     * @generated
     * @see #setStylingIdentifier(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getMapStylingType_StylingIdentifier()
     */
    EObject getStylingIdentifier();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MapStylingType#getStylingIdentifier <em>Styling Identifier</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Styling Identifier</em>' containment reference.
     * @generated
     * @see #getStylingIdentifier()
     */
    void setStylingIdentifier(EObject value);

    /**
     * Returns the value of the '<em><b>Styling URL</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Reference to a web-accessible resource that contains the styling information to be applied. This attribute shall contain a URL from which this input can be electronically retrieved.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Styling URL</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI" required="true"
     * extendedMetaData="kind='element' name='StylingURL' namespace='##targetNamespace'"
     * @generated
     * @see #setStylingURL(String)
     * @see net.opengis.tjs10.Tjs10Package#getMapStylingType_StylingURL()
     */
    String getStylingURL();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MapStylingType#getStylingURL <em>Styling URL</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Styling URL</em>' attribute.
     * @generated
     * @see #getStylingURL()
     */
    void setStylingURL(String value);

} // MapStylingType
