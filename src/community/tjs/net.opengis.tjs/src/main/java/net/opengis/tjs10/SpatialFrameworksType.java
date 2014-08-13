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
 * A representation of the model object '<em><b>Spatial Frameworks Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.SpatialFrameworksType#getFramework <em>Framework</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='SpatialFrameworks_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getSpatialFrameworksType()
 */
public interface SpatialFrameworksType extends EObject {
    /**
     * Returns the value of the '<em><b>Framework</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.FrameworkType}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Framework</em>' containment reference list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Framework</em>' containment reference list.
     * @model type="net.opengis.tjs10.FrameworkType" containment="true" required="true"
     * extendedMetaData="kind='element' name='Framework' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getSpatialFrameworksType_Framework()
     */
    EList getFramework();

} // SpatialFrameworksType
