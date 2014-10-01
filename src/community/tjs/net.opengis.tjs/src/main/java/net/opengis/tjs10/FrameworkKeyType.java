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
 * A representation of the model object '<em><b>Framework Key Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.FrameworkKeyType#getColumn <em>Column</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='FrameworkKey_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getFrameworkKeyType()
 */
public interface FrameworkKeyType extends EObject {
    /**
     * Returns the value of the '<em><b>Column</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.ColumnType}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Column</em>' containment reference list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Column</em>' containment reference list.
     * @model type="net.opengis.tjs10.ColumnType" containment="true" required="true"
     * extendedMetaData="kind='element' name='Column' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkKeyType_Column()
     */
    EList getColumn();

} // FrameworkKeyType
