/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;
import net.opengis.ows10.AcceptFormatsType;
import net.opengis.ows10.Ows10Factory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/ows:AcceptFormatsType.
 *
 * <pre><code>
 *  &lt;complexType name="AcceptFormatsType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Prioritized sequence of zero or more GetCapabilities operation response formats desired by client, with preferred formats listed first. Each response format shall be identified by its MIME type. See AcceptFormats parameter use subclause for more information. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" name="OutputFormat" type="ows:MimeType"/&gt;
 *      &lt;/sequence&gt;
 *  &lt;/complexType&gt;
 * </code></pre>
 *
 * @generated
 */
public class AcceptFormatsTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public AcceptFormatsTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.ACCEPTFORMATSTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return AcceptFormatsType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        AcceptFormatsType acceptFormats = owsfactory.createAcceptFormatsType();
        acceptFormats.getOutputFormat().addAll(node.getChildValues("OutputFormat"));

        return acceptFormats;
    }
}
