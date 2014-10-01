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
 * A representation of the model object '<em><b>Framework Key Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.FrameworkKeyType1#getColumn <em>Column</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkKeyType1#getComplete <em>Complete</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkKeyType1#getRelationship <em>Relationship</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='FrameworkKey_._1_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getFrameworkKeyType1()
 */
public interface FrameworkKeyType1 extends EObject {
    /**
     * Returns the value of the '<em><b>Column</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.ColumnType2}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifies a column that is used to form the framework key.  Where more than one of these elements is present then all of these columns are required to join the data table to the spatial framework.  The order of these elements defines the order of the "K" elements in the Rowset/Row structures below.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Column</em>' containment reference list.
     * @model type="net.opengis.tjs10.ColumnType2" containment="true" required="true"
     * extendedMetaData="kind='element' name='Column' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkKeyType1_Column()
     */
    EList getColumn();

    /**
     * Returns the value of the '<em><b>Complete</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifies if there is at least one record in the Attribute dataset for every record in the Framework dataset.  �true� indicates that this is the case. �false� indicates that some Keys in the Framework dataset cannot be found in the Attribute dataset.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Complete</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='complete'"
     * @generated
     * @see #setComplete(Object)
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkKeyType1_Complete()
     */
    Object getComplete();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkKeyType1#getComplete <em>Complete</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Complete</em>' attribute.
     * @generated
     * @see #getComplete()
     */
    void setComplete(Object value);

    /**
     * Returns the value of the '<em><b>Relationship</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifies if the relationship between the Framework and the Attribute datasets are 1:1 or 1:many.  �one� indicates that there is at most one record in the Attribute dataset for every key in the Framework dataset.  �many� indicates that there may be more than one record in the Attribute dataset for every key in the Framework dataset, in which case some preliminary processing is required before the join operation can take place.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Relationship</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='relationship'"
     * @generated
     * @see #setRelationship(Object)
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkKeyType1_Relationship()
     */
    Object getRelationship();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkKeyType1#getRelationship <em>Relationship</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Relationship</em>' attribute.
     * @generated
     * @see #getRelationship()
     */
    void setRelationship(Object value);

} // FrameworkKeyType1
