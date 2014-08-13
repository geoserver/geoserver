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
 * A representation of the model object '<em><b>UOM Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.UOMType#getShortForm <em>Short Form</em>}</li>
 * <li>{@link net.opengis.tjs10.UOMType#getLongForm <em>Long Form</em>}</li>
 * <li>{@link net.opengis.tjs10.UOMType#getReference <em>Reference</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='UOM_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getUOMType()
 */
public interface UOMType extends EObject {
    /**
     * Returns the value of the '<em><b>Short Form</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Short form of the unit of measure, suitable for charts or legends (e.g. "�C").
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Short Form</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='ShortForm' namespace='##targetNamespace'"
     * @generated
     * @see #setShortForm(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getUOMType_ShortForm()
     */
    EObject getShortForm();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.UOMType#getShortForm <em>Short Form</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Short Form</em>' containment reference.
     * @generated
     * @see #getShortForm()
     */
    void setShortForm(EObject value);

    /**
     * Returns the value of the '<em><b>Long Form</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Long form of the unit of measure, suitable for complete text descriptions (e.g. "degrees centigrade").
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Long Form</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='LongForm' namespace='##targetNamespace'"
     * @generated
     * @see #setLongForm(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getUOMType_LongForm()
     */
    EObject getLongForm();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.UOMType#getLongForm <em>Long Form</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Long Form</em>' containment reference.
     * @generated
     * @see #getLongForm()
     */
    void setLongForm(EObject value);

    /**
     * Returns the value of the '<em><b>Reference</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Reference to data or metadata recorded elsewhere, either external to this XML document or within it. Whenever practical, this attribute should be a URL from which this metadata can be electronically retrieved. Alternately, this attribute can reference a URN for well-known metadata. For example, such a URN could be a URN defined in the "ogc" URN namespace.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Reference</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI"
     * extendedMetaData="kind='attribute' name='reference' namespace='http://www.opengis.net/ows/1.1'"
     * @generated
     * @see #setReference(String)
     * @see net.opengis.tjs10.Tjs10Package#getUOMType_Reference()
     */
    String getReference();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.UOMType#getReference <em>Reference</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Reference</em>' attribute.
     * @generated
     * @see #getReference()
     */
    void setReference(String value);

} // UOMType
