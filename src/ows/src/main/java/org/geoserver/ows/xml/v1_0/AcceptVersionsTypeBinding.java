/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;
import net.opengis.ows10.AcceptVersionsType;
import net.opengis.ows10.Ows10Factory;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/ows:AcceptVersionsType.
 *
 * <pre><code>
 *  &lt;complexType name="AcceptVersionsType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Prioritized sequence of one or more specification versions accepted by client, with preferred versions listed first. See Version negotiation subclause for more information. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element maxOccurs="unbounded" name="Version" type="ows:VersionType"/&gt;
 *      &lt;/sequence&gt;
 *  &lt;/complexType&gt;
 *
 * </code></pre>
 *
 * @generated
 */
public class AcceptVersionsTypeBinding extends AbstractComplexEMFBinding {

    Ows10Factory owsfactory;

    public AcceptVersionsTypeBinding(Ows10Factory owsfactory) {
        super(owsfactory);
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.ACCEPTVERSIONSTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return AcceptVersionsType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        AcceptVersionsType acceptVersions = owsfactory.createAcceptVersionsType();
        acceptVersions.getVersion().addAll(node.getChildValues("Version"));

        return acceptVersions;
    }
}
