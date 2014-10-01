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
 * A representation of the model object '<em><b>Output Mechanisms Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.OutputMechanismsType#getMechanism <em>Mechanism</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='OutputMechanismsType' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getOutputMechanismsType()
 */
public interface OutputMechanismsType extends EObject {
    /**
     * Returns the value of the '<em><b>Mechanism</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.MechanismType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Mechanism by which the attribute data can be accessed once it has been joined to the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Mechanism</em>' containment reference list.
     * @model type="net.opengis.tjs10.MechanismType" containment="true" required="true"
     * extendedMetaData="kind='element' name='Mechanism' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getOutputMechanismsType_Mechanism()
     */
    EList getMechanism();

} // OutputMechanismsType
