/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.EObject;

import java.math.BigInteger;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Join Abilities Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getSpatialFrameworks <em>Spatial Frameworks</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getAttributeLimit <em>Attribute Limit</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getOutputMechanisms <em>Output Mechanisms</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getOutputStylings <em>Output Stylings</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getClassificationSchemaURL <em>Classification Schema URL</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#isUpdateSupported <em>Update Supported</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinAbilitiesType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='JoinAbilities_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType()
 */
public interface JoinAbilitiesType extends EObject {
    /**
     * Returns the value of the '<em><b>Spatial Frameworks</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Full description of all spatial frameworks to which attribute data can be joined.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Spatial Frameworks</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='SpatialFrameworks' namespace='##targetNamespace'"
     * @generated
     * @see #setSpatialFrameworks(SpatialFrameworksType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_SpatialFrameworks()
     */
    SpatialFrameworksType getSpatialFrameworks();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getSpatialFrameworks <em>Spatial Frameworks</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Spatial Frameworks</em>' containment reference.
     * @generated
     * @see #getSpatialFrameworks()
     */
    void setSpatialFrameworks(SpatialFrameworksType value);

    /**
     * Returns the value of the '<em><b>Attribute Limit</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Maximum number of attributes that can be joined simultaneously as part of a JoinData request.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Attribute Limit</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.PositiveInteger" required="true"
     * extendedMetaData="kind='element' name='AttributeLimit' namespace='##targetNamespace'"
     * @generated
     * @see #setAttributeLimit(BigInteger)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_AttributeLimit()
     */
    BigInteger getAttributeLimit();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getAttributeLimit <em>Attribute Limit</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Attribute Limit</em>' attribute.
     * @generated
     * @see #getAttributeLimit()
     */
    void setAttributeLimit(BigInteger value);

    /**
     * Returns the value of the '<em><b>Output Mechanisms</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * List of mechanisms by which the attribute data will be accessible once it has been joined to the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Output Mechanisms</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='OutputMechanisms' namespace='##targetNamespace'"
     * @generated
     * @see #setOutputMechanisms(OutputMechanismsType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_OutputMechanisms()
     */
    OutputMechanismsType getOutputMechanisms();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getOutputMechanisms <em>Output Mechanisms</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Output Mechanisms</em>' containment reference.
     * @generated
     * @see #getOutputMechanisms()
     */
    void setOutputMechanisms(OutputMechanismsType value);

    /**
     * Returns the value of the '<em><b>Output Stylings</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Unordered list of display or content styling instructions supported by the server and that can be applied if the AccessMechanisms of the requested output includes WMS.  If WMS is not supported by the server then this element shall not be present.  If WMS is supported and this element is not present, a default styling will be applied to the WMS layer.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Output Stylings</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='OutputStylings' namespace='##targetNamespace'"
     * @generated
     * @see #setOutputStylings(OutputStylingsType1)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_OutputStylings()
     */
    OutputStylingsType1 getOutputStylings();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getOutputStylings <em>Output Stylings</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Output Stylings</em>' containment reference.
     * @generated
     * @see #getOutputStylings()
     */
    void setOutputStylings(OutputStylingsType1 value);

    /**
     * Returns the value of the '<em><b>Classification Schema URL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL that returns an XML Schema document that specifying an XML structure for describing data classifications.  When included with a JoinData request, the contents of such an XML file can be applied by the server in order to produce the data classes used in the JoinData output.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Classification Schema URL</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='ClassificationSchemaURL' namespace='##targetNamespace'"
     * @generated
     * @see #setClassificationSchemaURL(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_ClassificationSchemaURL()
     */
    EObject getClassificationSchemaURL();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getClassificationSchemaURL <em>Classification Schema URL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Classification Schema URL</em>' containment reference.
     * @generated
     * @see #getClassificationSchemaURL()
     */
    void setClassificationSchemaURL(EObject value);

    /**
     * Returns the value of the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * GetCapabilities URL of the TJS server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Capabilities</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='attribute' name='capabilities'"
     * @generated
     * @see #setCapabilities(String)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_Capabilities()
     */
    String getCapabilities();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getCapabilities <em>Capabilities</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Capabilities</em>' attribute.
     * @generated
     * @see #getCapabilities()
     */
    void setCapabilities(String value);

    /**
     * Returns the value of the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * RFC 4646 language code of the human-readable text (e.g. "en-CA").
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Lang</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='lang' namespace='http://www.w3.org/XML/1998/namespace'"
     * @generated
     * @see #setLang(Object)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_Lang()
     */
    Object getLang();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getLang <em>Lang</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Lang</em>' attribute.
     * @generated
     * @see #getLang()
     */
    void setLang(Object value);

    /**
     * Returns the value of the '<em><b>Service</b></em>' attribute.
     * The default value is <code>"TJS"</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Service type identifier
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Service</em>' attribute.
     * @model default="TJS" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='service'"
     * @generated
     * @see #isSetService()
     * @see #unsetService()
     * @see #setService(Object)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_Service()
     */
    Object getService();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getService <em>Service</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Service</em>' attribute.
     * @generated
     * @see #isSetService()
     * @see #unsetService()
     * @see #getService()
     */
    void setService(Object value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getService <em>Service</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetService()
     * @see #getService()
     * @see #setService(Object)
     */
    void unsetService();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getService <em>Service</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Service</em>' attribute is set.
     * @generated
     * @see #unsetService()
     * @see #getService()
     * @see #setService(Object)
     */
    boolean isSetService();

    /**
     * Returns the value of the '<em><b>Update Supported</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Boolean that identifies if existing JoinData products can be updated by this service.  If "true" then subsequent identical JoinData requests will update existing JoinData products that were created by this service.  These updated products will then be available via the existing URLs of those products.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Update Supported</em>' attribute.
     * @model unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean" required="true"
     * extendedMetaData="kind='attribute' name='updateSupported'"
     * @generated
     * @see #isSetUpdateSupported()
     * @see #unsetUpdateSupported()
     * @see #setUpdateSupported(boolean)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_UpdateSupported()
     */
    boolean isUpdateSupported();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#isUpdateSupported <em>Update Supported</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Update Supported</em>' attribute.
     * @generated
     * @see #isSetUpdateSupported()
     * @see #unsetUpdateSupported()
     * @see #isUpdateSupported()
     */
    void setUpdateSupported(boolean value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#isUpdateSupported <em>Update Supported</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetUpdateSupported()
     * @see #isUpdateSupported()
     * @see #setUpdateSupported(boolean)
     */
    void unsetUpdateSupported();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#isUpdateSupported <em>Update Supported</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Update Supported</em>' attribute is set.
     * @generated
     * @see #unsetUpdateSupported()
     * @see #isUpdateSupported()
     * @see #setUpdateSupported(boolean)
     */
    boolean isSetUpdateSupported();

    /**
     * Returns the value of the '<em><b>Version</b></em>' attribute.
     * The default value is <code>"1.0"</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Version of the TJS interface specification implemented by the server (1.0)
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Version</em>' attribute.
     * @model default="1.0" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='version'"
     * @generated
     * @see #isSetVersion()
     * @see #unsetVersion()
     * @see #setVersion(Object)
     * @see net.opengis.tjs10.Tjs10Package#getJoinAbilitiesType_Version()
     */
    Object getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Version</em>' attribute.
     * @generated
     * @see #isSetVersion()
     * @see #unsetVersion()
     * @see #getVersion()
     */
    void setVersion(Object value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetVersion()
     * @see #getVersion()
     * @see #setVersion(Object)
     */
    void unsetVersion();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.JoinAbilitiesType#getVersion <em>Version</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Version</em>' attribute is set.
     * @generated
     * @see #unsetVersion()
     * @see #getVersion()
     * @see #setVersion(Object)
     */
    boolean isSetVersion();

} // JoinAbilitiesType
