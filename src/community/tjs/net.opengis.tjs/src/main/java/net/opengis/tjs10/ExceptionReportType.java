/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import net.opengis.ows11.ExceptionType;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Exception Report Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ExceptionReportType#getException <em>Exception</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='ExceptionReport_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getExceptionReportType()
 */
public interface ExceptionReportType extends EObject {
    /**
     * Returns the value of the '<em><b>Exception</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Error encountered during processing that prevented successful production of this output.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Exception</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Exception' namespace='http://www.opengis.net/ows/1.1'"
     * @generated
     * @see #setException(ExceptionType)
     * @see net.opengis.tjs10.Tjs10Package#getExceptionReportType_Exception()
     */
    ExceptionType getException();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ExceptionReportType#getException <em>Exception</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Exception</em>' containment reference.
     * @generated
     * @see #getException()
     */
    void setException(ExceptionType value);

} // ExceptionReportType
