/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;
import net.opengis.ows10.Ows10Factory;
import net.opengis.ows10.SectionsType;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/ows:SectionsType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="SectionsType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Unordered list of zero or more names of requested sections in complete service metadata document. Each Section value shall contain an allowed section name as specified by each OWS specification. See Sections parameter subclause for more information.  &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" name="Section" type="string"/&gt;
 *      &lt;/sequence&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class SectionsTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public SectionsTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.SECTIONSTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return SectionsType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        SectionsType sections = owsfactory.createSectionsType();
        sections.getSection().addAll(node.getChildValues("Section"));

        return sections;
    }
}
