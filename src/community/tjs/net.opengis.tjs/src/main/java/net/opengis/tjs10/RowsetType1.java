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
 * A representation of the model object '<em><b>Rowset Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <!-- begin-model-doc -->
 * Rowset type defines a section for a dataset. Rowset can be presented more than once. However the efficient use of Rowset will be once per GetData response
 * <!-- end-model-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.RowsetType1#getRow <em>Row</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Rowset_._1_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getRowsetType1()
 */
public interface RowsetType1 extends EObject {
    /**
     * Returns the value of the '<em><b>Row</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.RowType1}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Dataset Row
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Row</em>' containment reference list.
     * @model type="net.opengis.tjs10.RowType1" containment="true" required="true"
     * extendedMetaData="kind='element' name='Row' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getRowsetType1_Row()
     */
    EList getRow();

} // RowsetType1
