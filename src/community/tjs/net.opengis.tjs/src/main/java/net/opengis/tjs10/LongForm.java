/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>ShortForm</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.LongForm#getValue <em>Value</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='K_._type' kind='simple'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getKType()
 */
public interface LongForm extends EObject {
    /**
     * Returns the value of the '<em><b>Value</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Value</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Value</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="name=':0' kind='simple'"
     * @generated
     * @see #setValue(String)
     * @see net.opengis.tjs10.Tjs10Package#getKType_Value()
     */
    String getValue();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.LongForm#getValue <em>Value</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Value</em>' attribute.
     * @generated
     * @see #getValue()
     */
    void setValue(String value);

} // ShortForm
