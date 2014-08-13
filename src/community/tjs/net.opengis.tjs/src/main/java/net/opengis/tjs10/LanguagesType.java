/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Languages Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.LanguagesType#getLanguage <em>Language</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Languages_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getLanguagesType()
 */
public interface LanguagesType extends EObject {
    /**
     * Returns the value of the '<em><b>Language</b></em>' attribute list.
     * The list contents are of type {@link java.lang.String}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifier of a language used by the data(set) contents. This language identifier shall be as specified in IETF RFC 4646. When this element is omitted, the language used is not identified.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Language</em>' attribute list.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.Language" required="true"
     * extendedMetaData="kind='element' name='Language' namespace='http://www.opengis.net/ows/1.1'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getLanguagesType_Language()
     */
    EList getLanguage();

} // LanguagesType
