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
 * A representation of the model object '<em><b>Null Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.NullType1#getIdentifier <em>Identifier</em>}</li>
 * <li>{@link net.opengis.tjs10.NullType1#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.NullType1#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.NullType1#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.NullType1#getColor <em>Color</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Null_._1_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getNullType1()
 */
public interface NullType1 extends EObject {
    /**
     * Returns the value of the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Text string representing a null value, found in the V elements of this attribute
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Identifier</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Identifier' namespace='##targetNamespace'"
     * @generated
     * @see #setIdentifier(String)
     * @see net.opengis.tjs10.Tjs10Package#getNullType1_Identifier()
     */
    String getIdentifier();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.NullType1#getIdentifier <em>Identifier</em>}' attribute.
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
     * Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Title</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Title' namespace='##targetNamespace'"
     * @generated
     * @see #setTitle(String)
     * @see net.opengis.tjs10.Tjs10Package#getNullType1_Title()
     */
    String getTitle();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.NullType1#getTitle <em>Title</em>}' attribute.
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
     * One or more paragraphs of human-readable relevant text suitable for display in a pop-up window.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Abstract</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Abstract' namespace='##targetNamespace'"
     * @generated
     * @see #setAbstract(AbstractType)
     * @see net.opengis.tjs10.Tjs10Package#getNullType1_Abstract()
     */
    AbstractType getAbstract();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.NullType1#getAbstract <em>Abstract</em>}' containment reference.
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
     * @see net.opengis.tjs10.Tjs10Package#getNullType1_Documentation()
     */
    String getDocumentation();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.NullType1#getDocumentation <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Documentation</em>' attribute.
     * @generated
     * @see #getDocumentation()
     */
    void setDocumentation(String value);

    /**
     * Returns the value of the '<em><b>Color</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Hex code for a color that is suggested for cartographic portrayal of this null value.  E.g."CCFFCC"
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Color</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType"
     * extendedMetaData="kind='attribute' name='color'"
     * @generated
     * @see #setColor(Object)
     * @see net.opengis.tjs10.Tjs10Package#getNullType1_Color()
     */
    Object getColor();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.NullType1#getColor <em>Color</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Color</em>' attribute.
     * @generated
     * @see #getColor()
     */
    void setColor(Object value);

} // NullType1
