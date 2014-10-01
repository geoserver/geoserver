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
 * A representation of the model object '<em><b>Parameter Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ParameterType#getValue <em>Value</em>}</li>
 * <li>{@link net.opengis.tjs10.ParameterType#getName <em>Name</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Parameter_._type' kind='simple'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getParameterType()
 */
public interface ParameterType extends EObject {
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
     * @see net.opengis.tjs10.Tjs10Package#getParameterType_Value()
     */
    String getValue();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ParameterType#getValue <em>Value</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Value</em>' attribute.
     * @generated
     * @see #getValue()
     */
    void setValue(String value);

    /**
     * Returns the value of the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifier for this parameter as defined by the service delivering the output of the JoinData operation.  For a WMS output this attribute shall be populated with the string "layers", thus providing sufficient information to allow the client to construct the "Layers=" parameter of a GetMap request.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Name</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='name'"
     * @generated
     * @see #setName(Object)
     * @see net.opengis.tjs10.Tjs10Package#getParameterType_Name()
     */
    Object getName();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ParameterType#getName <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Name</em>' attribute.
     * @generated
     * @see #getName()
     */
    void setName(Object value);

} // ParameterType
