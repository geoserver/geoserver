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
 * A representation of the model object '<em><b>Output Stylings Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.OutputStylingsType#getStyling <em>Styling</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='OutputStylingsType' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getOutputStylingsType()
 */
public interface OutputStylingsType extends EObject {
    /**
     * Returns the value of the '<em><b>Styling</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.StylingType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Describes a form of styling instruction supported by this server. (e.g. SLD)
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Styling</em>' containment reference list.
     * @model type="net.opengis.tjs10.StylingType" containment="true" required="true"
     * extendedMetaData="kind='element' name='Styling' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getOutputStylingsType_Styling()
     */
    EList getStyling();

} // OutputStylingsType
