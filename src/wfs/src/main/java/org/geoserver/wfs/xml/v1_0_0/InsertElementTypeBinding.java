/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.net.URI;
import javax.xml.namespace.QName;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.wfs.WFSException;
import org.geotools.gml2.bindings.GML2ParsingUtils;
import org.geotools.gml3.GML;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;

/**
 * Binding object for the type http://www.opengis.net/wfs:InsertElementType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="InsertElementType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element maxOccurs="unbounded" ref="gml:_Feature"/&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"/&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class InsertElementTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;

    public InsertElementTypeBinding(WfsFactory wfsfactory) {
        super(wfsfactory);
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.INSERTELEMENTTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return InsertElementTypeBinding.class;
    }

    public void initializeChildContext(
            ElementInstance childInstance, Node node, MutablePicoContainer context) {
        // if an srsName is set for this geometry, put it in the context for
        // children, so they can use it as well
        if (node.hasAttribute("srsName")) {
            try {
                CoordinateReferenceSystem crs = GML2ParsingUtils.crs(node);
                if (crs != null) {
                    context.registerComponentInstance(CoordinateReferenceSystem.class, crs);
                }
            } catch (Exception e) {
                throw new WFSException(e, "InvalidParameterValue");
            }
        }
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        InsertElementType insertElement = wfsfactory.createInsertElementType();

        // features
        insertElement.getFeature().addAll(node.getChildValues(SimpleFeature.class));

        // handle
        if (node.hasAttribute("handle")) {
            insertElement.setHandle((String) node.getAttributeValue("handle"));
        }

        // NOTE: officially this is not supported for wfs 1.0, but we support it
        // here as an extension to wfs 1.0, also since its not actualy in the
        // schema it comes to us as a string, not a uri
        // &lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
        if (node.hasAttribute("srsName")) {
            String srsName = (String) node.getAttributeValue("srsName");
            insertElement.setSrsName(new URI(srsName));
        }
        return insertElement;
    }

    public Object getProperty(Object object, QName name) throws Exception {
        InsertElementType insert = (InsertElementType) object;

        if (GML._Feature.equals(name)) {
            return insert.getFeature();
        }

        return super.getProperty(object, name);
    }
}
