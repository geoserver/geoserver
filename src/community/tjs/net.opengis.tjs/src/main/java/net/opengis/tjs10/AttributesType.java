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
 * A representation of the model object '<em><b>Attributes Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.AttributesType#getColumn <em>Column</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Attributes_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getAttributesType()
 */
public interface AttributesType extends EObject {
    /**
     * Returns the value of the '<em><b>Column</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.ColumnType1}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Describes a descriptor column or data column in the dataset.  The order of multiple occurances of this element in a GetData response is determined by the order of the attributes listed in the request.  The order of these elements defines the order of the "V" elements in the Rowset/Row structure below.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Column</em>' containment reference list.
     * @model type="net.opengis.tjs10.ColumnType1" containment="true" required="true"
     * extendedMetaData="kind='element' name='Column' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getAttributesType_Column()
     */
    EList getColumn();

} // AttributesType
