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
 * A representation of the model object '<em><b>Row Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.RowType1#getK <em>K</em>}</li>
 * <li>{@link net.opengis.tjs10.RowType1#getV <em>V</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Row_._1_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getRowType1()
 */
public interface RowType1 extends EObject {
    /**
     * Returns the value of the '<em><b>K</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.KType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Spatial Key for this row.  For the GetData response, when there is more than one "K" element they are ordered according to the same sequence as the "FrameworkKey" elements of the "Columnset" structure.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>K</em>' containment reference list.
     * @model type="net.opengis.tjs10.KType" containment="true" required="true"
     * extendedMetaData="kind='element' name='K' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getRowType1_K()
     */
    EList getK();

    /**
     * Returns the value of the '<em><b>V</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.VType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Value of a attribute (i.e. data) applicable to the spatial feature identified by the "K" elements of this row. When there is more than one "V" element, they are ordered according to the same sequence as the "Column" elements of the "Columnset" structure above.  When this value is null (indicated with the null attribute) an identification of the reason may be included in the content of this element.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>V</em>' containment reference list.
     * @model type="net.opengis.tjs10.VType" containment="true" required="true"
     * extendedMetaData="kind='element' name='V' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getRowType1_V()
     */
    EList getV();

} // RowType1
