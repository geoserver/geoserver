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
 * A representation of the model object '<em><b>Reference Date Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ReferenceDateType#getValue <em>Value</em>}</li>
 * <li>{@link net.opengis.tjs10.ReferenceDateType#getStartDate <em>Start Date</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='ReferenceDate_._type' kind='simple'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getReferenceDateType()
 */
public interface ReferenceDateType extends EObject {
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
     * @see net.opengis.tjs10.Tjs10Package#getReferenceDateType_Value()
     */
    String getValue();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ReferenceDateType#getValue <em>Value</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Value</em>' attribute.
     * @generated
     * @see #getValue()
     */
    void setValue(String value);

    /**
     * Returns the value of the '<em><b>Start Date</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Start date of a range of time to which the framework/dataset applies.  Valid content is a date field of the form http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#gYear, gYearMonth, date, or dateTime.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Start Date</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='attribute' name='startDate'"
     * @generated
     * @see #setStartDate(String)
     * @see net.opengis.tjs10.Tjs10Package#getReferenceDateType_StartDate()
     */
    String getStartDate();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ReferenceDateType#getStartDate <em>Start Date</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Start Date</em>' attribute.
     * @generated
     * @see #getStartDate()
     */
    void setStartDate(String value);

} // ReferenceDateType
