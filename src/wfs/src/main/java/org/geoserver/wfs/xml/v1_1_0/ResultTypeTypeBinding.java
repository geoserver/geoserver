/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractSimpleBinding;
import org.geotools.xsd.InstanceComponent;

/**
 * Binding object for the type http://www.opengis.net/wfs:ResultTypeType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:simpleType name="ResultTypeType"&gt;
 *      &lt;xsd:restriction base="xsd:string"&gt;
 *          &lt;xsd:enumeration value="results"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    Indicates that a complete response should be generated
 *                    by the WFS.  That is, all response feature instances
 *                    should be returned to the client.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:enumeration&gt;
 *          &lt;xsd:enumeration value="hits"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    Indicates that an empty response should be generated with
 *                    the "numberOfFeatures" attribute set (i.e. no feature
 *                    instances should be returned).  In this manner a client may
 *                    determine the number of feature instances that a GetFeature
 *                    request will return without having to actually get the
 *                    entire result set back.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:enumeration&gt;
 *      &lt;/xsd:restriction&gt;
 *  &lt;/xsd:simpleType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class ResultTypeTypeBinding extends AbstractSimpleBinding {
    WfsFactory wfsfactory;

    public ResultTypeTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.RESULTTYPETYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return ResultTypeType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value) throws Exception {
        if ("results".equals(value)) {
            return ResultTypeType.RESULTS_LITERAL;
        }

        if ("hits".equals(value)) {
            return ResultTypeType.HITS_LITERAL;
        }

        return null;
    }
}
