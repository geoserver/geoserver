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
 * A representation of the model object '<em><b>Joined Outputs Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.JoinedOutputsType#getOutput <em>Output</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='JoinedOutputs_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getJoinedOutputsType()
 */
public interface JoinedOutputsType extends EObject {
    /**
     * Returns the value of the '<em><b>Output</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.OutputType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Unordered list of all the outputs that have been or will be produced by this operation.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Output</em>' containment reference list.
     * @model type="net.opengis.tjs10.OutputType" containment="true" required="true"
     * extendedMetaData="kind='element' name='Output' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getJoinedOutputsType_Output()
     */
    EList getOutput();

} // JoinedOutputsType
