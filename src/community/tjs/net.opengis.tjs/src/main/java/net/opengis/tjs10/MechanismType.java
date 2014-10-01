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
 * A representation of the model object '<em><b>Mechanism Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.MechanismType#getIdentifier <em>Identifier</em>}</li>
 * <li>{@link net.opengis.tjs10.MechanismType#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.MechanismType#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.MechanismType#getReference <em>Reference</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Mechanism_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getMechanismType()
 */
public interface MechanismType extends EObject {
    /**
     * Returns the value of the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Name which uniquely identifies this type of access mechanism supported by this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Identifier</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Identifier' namespace='##targetNamespace'"
     * @generated
     * @see #setIdentifier(String)
     * @see net.opengis.tjs10.Tjs10Package#getMechanismType_Identifier()
     */
    String getIdentifier();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MechanismType#getIdentifier <em>Identifier</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Identifier</em>' attribute.
     * @generated
     * @see #getIdentifier()
     */
    void setIdentifier(String value);

    /**
     * Returns the value of the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Human-readable title which uniquely identifies the type of access mechanism supported by this server.  Must be suitable for display in a pick-list to a user.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Title</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Title' namespace='##targetNamespace'"
     * @generated
     * @see #setTitle(String)
     * @see net.opengis.tjs10.Tjs10Package#getMechanismType_Title()
     */
    String getTitle();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MechanismType#getTitle <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Title</em>' attribute.
     * @generated
     * @see #getTitle()
     */
    void setTitle(String value);

    /**
     * Returns the value of the '<em><b>Abstract</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Human-readable description of the type of access mechanism, suitable for display to a user seeking information about this type of access mechanism.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Abstract</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Abstract' namespace='##targetNamespace'"
     * @generated
     * @see #setAbstract(String)
     * @see net.opengis.tjs10.Tjs10Package#getMechanismType_Abstract()
     */
    String getAbstract();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MechanismType#getAbstract <em>Abstract</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Abstract</em>' attribute.
     * @generated
     * @see #getAbstract()
     */
    void setAbstract(String value);

    /**
     * Returns the value of the '<em><b>Reference</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL that defines the access mechanism.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Reference</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI" required="true"
     * extendedMetaData="kind='element' name='Reference' namespace='##targetNamespace'"
     * @generated
     * @see #setReference(String)
     * @see net.opengis.tjs10.Tjs10Package#getMechanismType_Reference()
     */
    String getReference();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.MechanismType#getReference <em>Reference</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Reference</em>' attribute.
     * @generated
     * @see #getReference()
     */
    void setReference(String value);

} // MechanismType
