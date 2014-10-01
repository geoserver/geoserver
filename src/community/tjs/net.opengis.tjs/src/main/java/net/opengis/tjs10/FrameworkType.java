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
 * A representation of the model object '<em><b>Framework Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.FrameworkType#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getOrganization <em>Organization</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getReferenceDate <em>Reference Date</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getVersion <em>Version</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getFrameworkKey <em>Framework Key</em>}</li>
 * <li>{@link net.opengis.tjs10.FrameworkType#getBoundingCoordinates <em>Bounding Coordinates</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Framework_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getFrameworkType()
 */
public interface FrameworkType extends EObject {
    /**
     * Returns the value of the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI of the spatial framework.  Normally a resolvable URL or a URN.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework URI</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='element' name='FrameworkURI' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_FrameworkURI()
     */
    String getFrameworkURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getFrameworkURI <em>Framework URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework URI</em>' attribute.
     * @generated
     * @see #getFrameworkURI()
     */
    void setFrameworkURI(String value);

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
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_Organization()
     */
    String getOrganization();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getOrganization <em>Organization</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_Title()
     */
    String getTitle();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getTitle <em>Title</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_Abstract()
     */
    AbstractType getAbstract();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getAbstract <em>Abstract</em>}' containment reference.
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
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_ReferenceDate()
     */
    ReferenceDateType getReferenceDate();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getReferenceDate <em>Reference Date</em>}' containment reference.
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
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_Version()
     */
    String getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getVersion <em>Version</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_Documentation()
     */
    String getDocumentation();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getDocumentation <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Documentation</em>' attribute.
     * @generated
     * @see #getDocumentation()
     */
    void setDocumentation(String value);

    /**
     * Returns the value of the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Describes the common key field in the spatial framework dataset through which data can be joined.  The values of this key populate the 'Rowset/Row/K' elements in the GetData response.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework Key</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='FrameworkKey' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkKey(FrameworkKeyType)
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_FrameworkKey()
     */
    FrameworkKeyType getFrameworkKey();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getFrameworkKey <em>Framework Key</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework Key</em>' containment reference.
     * @generated
     * @see #getFrameworkKey()
     */
    void setFrameworkKey(FrameworkKeyType value);

    /**
     * Returns the value of the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifies the bounding coordinates of the spatial framework using the WGS84 CRS.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Bounding Coordinates</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='BoundingCoordinates' namespace='##targetNamespace'"
     * @generated
     * @see #setBoundingCoordinates(BoundingCoordinatesType)
     * @see net.opengis.tjs10.Tjs10Package#getFrameworkType_BoundingCoordinates()
     */
    BoundingCoordinatesType getBoundingCoordinates();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.FrameworkType#getBoundingCoordinates <em>Bounding Coordinates</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Bounding Coordinates</em>' containment reference.
     * @generated
     * @see #getBoundingCoordinates()
     */
    void setBoundingCoordinates(BoundingCoordinatesType value);

} // FrameworkType
