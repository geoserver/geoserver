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
 * A representation of the model object '<em><b>Dataset Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.DatasetType#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.DatasetType#getOrganization <em>Organization</em>}</li>
 * <li>{@link net.opengis.tjs10.DatasetType#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.DatasetType#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.DatasetType#getReferenceDate <em>Reference Date</em>}</li>
 * <li>{@link net.opengis.tjs10.DatasetType#getVersion <em>Version</em>}</li>
 * <li>{@link net.opengis.tjs10.DatasetType#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.DatasetType#getDescribeDataRequest <em>Describe Data Request</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Dataset_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDatasetType()
 */
public interface DatasetType extends EObject {
    /**
     * Returns the value of the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI of the attribute dataset.  Normally a resolvable URL or a URN.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Dataset URI</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='DatasetURI' namespace='##targetNamespace'"
     * @generated
     * @see #setDatasetURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_DatasetURI()
     */
    String getDatasetURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getDatasetURI <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Dataset URI</em>' attribute.
     * @generated
     * @see #getDatasetURI()
     */
    void setDatasetURI(String value);

    /**
     * Returns the value of the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Human-readable name of the organization responsible for maintaining this object.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Organization</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Organization' namespace='##targetNamespace'"
     * @generated
     * @see #setOrganization(String)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_Organization()
     */
    String getOrganization();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getOrganization <em>Organization</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Organization</em>' attribute.
     * @generated
     * @see #getOrganization()
     */
    void setOrganization(String value);

    /**
     * Returns the value of the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Title</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Title' namespace='##targetNamespace'"
     * @generated
     * @see #setTitle(String)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_Title()
     */
    String getTitle();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getTitle <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Title</em>' attribute.
     * @generated
     * @see #getTitle()
     */
    void setTitle(String value);

    /**
     * Returns the value of the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * One or more paragraphs of human-readable relevant text suitable for display in a pop-up window.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Abstract</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Abstract' namespace='##targetNamespace'"
     * @generated
     * @see #setAbstract(AbstractType)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_Abstract()
     */
    AbstractType getAbstract();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getAbstract <em>Abstract</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Abstract</em>' containment reference.
     * @generated
     * @see #getAbstract()
     */
    void setAbstract(AbstractType value);

    /**
     * Returns the value of the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Point in time to which the Framework/Dataset applies.  If the startDate attribute is included then the contents of this element describes a range of time (from "startDate" to "ReferenceDate") to which the framework/dataset applies.  Valid content is a date field of the form http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#gYear, gYearMonth, date, or dateTime.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Reference Date</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='ReferenceDate' namespace='##targetNamespace'"
     * @generated
     * @see #setReferenceDate(ReferenceDateType)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_ReferenceDate()
     */
    ReferenceDateType getReferenceDate();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getReferenceDate <em>Reference Date</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Reference Date</em>' containment reference.
     * @generated
     * @see #getReferenceDate()
     */
    void setReferenceDate(ReferenceDateType value);

    /**
     * Returns the value of the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Version identifier for this Framework / Dataset.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Version</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='Version' namespace='##targetNamespace'"
     * @generated
     * @see #setVersion(String)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_Version()
     */
    String getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Version</em>' attribute.
     * @generated
     * @see #getVersion()
     */
    void setVersion(String value);

    /**
     * Returns the value of the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to a web-accessible resource which contains further information describing this object.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Documentation</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI"
     * extendedMetaData="kind='element' name='Documentation' namespace='##targetNamespace'"
     * @generated
     * @see #setDocumentation(String)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_Documentation()
     */
    String getDocumentation();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getDocumentation <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Documentation</em>' attribute.
     * @generated
     * @see #getDocumentation()
     */
    void setDocumentation(String value);

    /**
     * Returns the value of the '<em><b>Describe Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to the DescribeData request for this dataset.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Data Request</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='DescribeDataRequest' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeDataRequest(DescribeDataRequestType)
     * @see net.opengis.tjs10.Tjs10Package#getDatasetType_DescribeDataRequest()
     */
    DescribeDataRequestType getDescribeDataRequest();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DatasetType#getDescribeDataRequest <em>Describe Data Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Data Request</em>' containment reference.
     * @generated
     * @see #getDescribeDataRequest()
     */
    void setDescribeDataRequest(DescribeDataRequestType value);

} // DatasetType
