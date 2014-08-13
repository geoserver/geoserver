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
 * A representation of the model object '<em><b>VType</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.VType#getValue <em>Value</em>}</li>
 * <li>{@link net.opengis.tjs10.VType#getAid <em>Aid</em>}</li>
 * <li>{@link net.opengis.tjs10.VType#isNull <em>Null</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='V_._type' kind='simple'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getVType()
 */
public interface VType extends EObject {
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
     * @see net.opengis.tjs10.Tjs10Package#getVType_Value()
     */
    String getValue();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.VType#getValue <em>Value</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Value</em>' attribute.
     * @generated
     * @see #getValue()
     */
    void setValue(String value);

    /**
     * Returns the value of the '<em><b>Aid</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Attribute identifier, namely the corresponding AttributeName
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Aid</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='attribute' name='aid'"
     * @generated
     * @see #setAid(String)
     * @see net.opengis.tjs10.Tjs10Package#getVType_Aid()
     */
    String getAid();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.VType#getAid <em>Aid</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Aid</em>' attribute.
     * @generated
     * @see #getAid()
     */
    void setAid(String value);

    /**
     * Returns the value of the '<em><b>Null</b></em>' attribute.
     * The default value is <code>"false"</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Boolean value, when present and "true" indicates that this particular value is missing for some reason, and the contents of the element must be processed accordingly.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Null</em>' attribute.
     * @model default="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
     * extendedMetaData="kind='attribute' name='null'"
     * @generated
     * @see #isSetNull()
     * @see #unsetNull()
     * @see #setNull(boolean)
     * @see net.opengis.tjs10.Tjs10Package#getVType_Null()
     */
    boolean isNull();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.VType#isNull <em>Null</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Null</em>' attribute.
     * @generated
     * @see #isSetNull()
     * @see #unsetNull()
     * @see #isNull()
     */
    void setNull(boolean value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.VType#isNull <em>Null</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetNull()
     * @see #isNull()
     * @see #setNull(boolean)
     */
    void unsetNull();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.VType#isNull <em>Null</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Null</em>' attribute is set.
     * @generated
     * @see #unsetNull()
     * @see #isNull()
     * @see #setNull(boolean)
     */
    boolean isSetNull();

} // VType
