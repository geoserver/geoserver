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
 * A representation of the model object '<em><b>Styling Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.StylingType#getIdentifier <em>Identifier</em>}</li>
 * <li>{@link net.opengis.tjs10.StylingType#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.StylingType#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.StylingType#getReference <em>Reference</em>}</li>
 * <li>{@link net.opengis.tjs10.StylingType#getSchema <em>Schema</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Styling_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getStylingType()
 */
public interface StylingType extends EObject {
    /**
     * Returns the value of the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Name that uniquely identifies this type of styling instructions supported by this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Identifier</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Identifier' namespace='##targetNamespace'"
     * @generated
     * @see #setIdentifier(String)
     * @see net.opengis.tjs10.Tjs10Package#getStylingType_Identifier()
     */
    String getIdentifier();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StylingType#getIdentifier <em>Identifier</em>}' attribute.
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
     * Human-readable title that uniquely identifies the type of styling instructions supported by this server.  Must be suitable for display in a pick-list to a user.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Title</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Title' namespace='##targetNamespace'"
     * @generated
     * @see #setTitle(String)
     * @see net.opengis.tjs10.Tjs10Package#getStylingType_Title()
     */
    String getTitle();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StylingType#getTitle <em>Title</em>}' attribute.
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
     * Human-readable description of the type of styling instructions, suitable for display to a user seeking information about this type of styling instruction.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Abstract</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Abstract' namespace='##targetNamespace'"
     * @generated
     * @see #setAbstract(String)
     * @see net.opengis.tjs10.Tjs10Package#getStylingType_Abstract()
     */
    String getAbstract();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StylingType#getAbstract <em>Abstract</em>}' attribute.
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
     * URL that defines the styling instructions.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Reference</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI" required="true"
     * extendedMetaData="kind='element' name='Reference' namespace='##targetNamespace'"
     * @generated
     * @see #setReference(String)
     * @see net.opengis.tjs10.Tjs10Package#getStylingType_Reference()
     */
    String getReference();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StylingType#getReference <em>Reference</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Reference</em>' attribute.
     * @generated
     * @see #getReference()
     */
    void setReference(String value);

    /**
     * Returns the value of the '<em><b>Schema</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Reference to a definition of XML elements or types supported for this styling instruction (e.g., a URL which returns the XSD for SLD 1.0). This parameter shall be included when the styling instructions are XML encoded using an XML schema. When included, the input/output shall validate against the referenced XML Schema. This element shall be omitted if Schema does not apply to this form of styling instruction. Note: If this styling instruction uses a profile of a larger schema, the server administrator should provide that schema profile for validation purposes.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Schema</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI"
     * extendedMetaData="kind='element' name='Schema' namespace='##targetNamespace'"
     * @generated
     * @see #setSchema(String)
     * @see net.opengis.tjs10.Tjs10Package#getStylingType_Schema()
     */
    String getSchema();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StylingType#getSchema <em>Schema</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Schema</em>' attribute.
     * @generated
     * @see #getSchema()
     */
    void setSchema(String value);

} // StylingType
