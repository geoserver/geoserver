/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.BaseRequestType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.ecore.EObject;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.EMFUtils;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:BaseRequestType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType abstract="true" name="BaseRequestType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              XML encoded WFS operation request base, for all operations
 *              except GetCapabilities.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:attribute default="WFS" name="service" type="ows:ServiceType" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                The service attribute is included to support service
 *                endpoints that implement more than one OGC service.
 *                For example, a single CGI that implements WMS, WFS
 *                and WCS services.
 *                The endpoint can inspect the value of this attribute
 *                to figure out which service should process the request.
 *                The value WFS indicates that a web feature service should
 *                process the request.
 *                This parameter is somewhat redundant in the XML encoding
 *                since the request namespace can be used to determine
 *                which service should process any give request.  For example,
 *                wfs:GetCapabilities and easily be distinguished from
 *                wcs:GetCapabilities using the namespaces.
 *             &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute default="1.1.0" name="version" type="xsd:string" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The version attribute is used to indicate the version of the
 *                 WFS specification that a request conforms to.  All requests in
 *                 this schema conform to V1.1 of the WFS specification.
 *                 For WFS implementations that support more than one version of
 *                 a WFS sepcification ... if the version attribute is not
 *                 specified then the service should assume that the request
 *                 conforms to greatest available specification version.
 *             &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The handle attribute allows a client application
 *                 to assign a client-generated request identifier
 *                 to a WFS request.  The handle is included to
 *                 facilitate error reporting.  A WFS may report the
 *                 handle in an exception report to identify the
 *                 offending request or action.  If the handle is not
 *                 present, then the WFS may employ other means to
 *                 localize the error (e.g. line numbers).
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class BaseRequestTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public BaseRequestTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.BASEREQUESTTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return BaseRequestType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        // this binding needs to be executed after the child
        EObject request = (EObject) value;

        // &lt;xsd:attribute default="WFS" name="service" type="ows:ServiceType" use="optional"&gt;
        if (node.hasAttribute("service")) {
            String service = (String) node.getAttributeValue("service");

            if ((service != null) && !"".equals(service.trim())) {
                EMFUtils.set(request, "service", node.getAttributeValue("service"));
            } else {
                EMFUtils.set(request, "service", "WFS");
            }
        } else {
            EMFUtils.set(request, "service", "WFS");
        }

        // &lt;xsd:attribute default="1.1.0" name="version" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("version")) {
            String version = (String) node.getAttributeValue("version");

            if ((version != null) && !"".equals(version.trim())) {
                EMFUtils.set(request, "version", node.getAttributeValue("version"));
            } else {
                EMFUtils.set(request, "version", "1.1.0");
            }
        } else {
            EMFUtils.set(request, "version", "1.1.0");
        }

        // &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("handle")) {
            EMFUtils.set(request, "handle", node.getAttributeValue("handle"));
        }

        return request;
    }
}
