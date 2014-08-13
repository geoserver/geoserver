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
 * A representation of the model object '<em><b>Accept Versions Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <!-- begin-model-doc -->
 * When omitted, server shall return latest supported version.
 * <!-- end-model-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.AcceptVersionsType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='AcceptVersions_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getAcceptVersionsType()
 */
public interface AcceptVersionsType extends EObject {
    /**
     * Returns the value of the '<em><b>Version</b></em>' attribute list.
     * The list contents are of type {@link net.opengis.tjs10.VersionType1}.
     * The literals are from the enumeration {@link net.opengis.tjs10.VersionType1}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Specification version for the TJS GetCapabilities operation. The string value shall contain one "version" value.  Version numbering is similar to OWS 1.1 except the version number contains only two non-negative integers separated by decimal points, in the form "x.y", where the integer x is the major version and y is the minor version.  Currently version "1.0" is the only valid value for this element.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Version</em>' attribute list.
     * @model unique="false" dataType="net.opengis.tjs10.VersionType1" required="true"
     * extendedMetaData="kind='element' name='Version' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.VersionType1
     * @see net.opengis.tjs10.Tjs10Package#getAcceptVersionsType_Version()
     */
    EList getVersion();

} // AcceptVersionsType
