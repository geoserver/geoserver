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
 * A representation of the model object '<em><b>Output Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.OutputType#getMechanism <em>Mechanism</em>}</li>
 * <li>{@link net.opengis.tjs10.OutputType#getResource <em>Resource</em>}</li>
 * <li>{@link net.opengis.tjs10.OutputType#getExceptionReport <em>Exception Report</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Output_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getOutputType()
 */
public interface OutputType extends EObject {
    /**
     * Returns the value of the '<em><b>Mechanism</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The access mechanism by which the joined data has been made available.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Mechanism</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Mechanism' namespace='##targetNamespace'"
     * @generated
     * @see #setMechanism(MechanismType)
     * @see net.opengis.tjs10.Tjs10Package#getOutputType_Mechanism()
     */
    MechanismType getMechanism();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.OutputType#getMechanism <em>Mechanism</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Mechanism</em>' containment reference.
     * @generated
     * @see #getMechanism()
     */
    void setMechanism(MechanismType value);

    /**
     * Returns the value of the '<em><b>Resource</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Reference to a web-accessible resource that was created by the JoinData operation.  This element shall be populated once the output has been successfully produced.  Prior to that time the content of the subelements may be empty.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Resource</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Resource' namespace='##targetNamespace'"
     * @generated
     * @see #setResource(ResourceType)
     * @see net.opengis.tjs10.Tjs10Package#getOutputType_Resource()
     */
    ResourceType getResource();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.OutputType#getResource <em>Resource</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Resource</em>' containment reference.
     * @generated
     * @see #getResource()
     */
    void setResource(ResourceType value);

    /**
     * Returns the value of the '<em><b>Exception Report</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Unordered list of one or more errors encountered during the JoinData operation for this output.  These Exception elements shall be interpreted by clients as being independent of one another (not hierarchical).  This element is populated when the production of this output did not succeed.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Exception Report</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='ExceptionReport' namespace='##targetNamespace'"
     * @generated
     * @see #setExceptionReport(ExceptionReportType)
     * @see net.opengis.tjs10.Tjs10Package#getOutputType_ExceptionReport()
     */
    ExceptionReportType getExceptionReport();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.OutputType#getExceptionReport <em>Exception Report</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Exception Report</em>' containment reference.
     * @generated
     * @see #getExceptionReport()
     */
    void setExceptionReport(ExceptionReportType value);

} // OutputType
