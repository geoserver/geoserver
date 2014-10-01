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
 * A representation of the model object '<em><b>Resource Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.ResourceType#getURL <em>URL</em>}</li>
 * <li>{@link net.opengis.tjs10.ResourceType#getParameter <em>Parameter</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Resource_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getResourceType()
 */
public interface ResourceType extends EObject {
    /**
     * Returns the value of the '<em><b>URL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL from which this resource can be electronically retrieved, or from which a document can be retrieved that indicates access details for the resource (such as a OGC Capabilities document).  For OGC web services this shall be the complete GetCapabilities URL.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>URL</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='URL' namespace='##targetNamespace'"
     * @generated
     * @see #setURL(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getResourceType_URL()
     */
    EObject getURL();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.ResourceType#getURL <em>URL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>URL</em>' containment reference.
     * @generated
     * @see #getURL()
     */
    void setURL(EObject value);

    /**
     * Returns the value of the '<em><b>Parameter</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.ParameterType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Parameter that may need to be included the HTTP requests to a web service identified by the URL parameter above.  For a WMS output there shall be one occurance of this element, and it shall be populated with the name of the layer produced by the JoinData operation.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Parameter</em>' containment reference list.
     * @model type="net.opengis.tjs10.ParameterType" containment="true"
     * extendedMetaData="kind='element' name='Parameter' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getResourceType_Parameter()
     */
    EList getParameter();

} // ResourceType
