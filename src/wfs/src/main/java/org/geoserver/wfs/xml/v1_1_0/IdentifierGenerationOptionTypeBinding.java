/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.IdentifierGenerationOptionType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractSimpleBinding;
import org.geotools.xsd.InstanceComponent;

/**
 * Binding object for the type http://www.opengis.net/wfs:IdentifierGenerationOptionType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:simpleType name="IdentifierGenerationOptionType"&gt;
 *      &lt;xsd:restriction base="xsd:string"&gt;
 *          &lt;xsd:enumeration value="UseExisting"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The UseExsiting value indicates that WFS should not
 *                    generate a new feature identifier for the feature
 *                    being inserted into the repositry.  Instead, the WFS
 *                    should use the identifier encoded if the feature.
 *                    If a duplicate exists then the WFS should raise an
 *                    exception.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:enumeration&gt;
 *          &lt;xsd:enumeration value="ReplaceDuplicate"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The ReplaceDuplicate value indicates that WFS should
 *                    not generate a new feature identifier for the feature
 *                    being inserted into the repositry.  Instead, the WFS
 *                    should use the identifier encoded if the feature.
 *                    If a duplicate exists then the WFS should replace the
 *                    existing feature instance with the one encoded in the
 *                    Insert action.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:enumeration&gt;
 *          &lt;xsd:enumeration value="GenerateNew"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The GenerateNew value indicates that WFS should
 *                    generate a new unique feature identifier for the
 *                    feature being inserted into the repositry.
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
public class IdentifierGenerationOptionTypeBinding extends AbstractSimpleBinding {
    WfsFactory wfsfactory;

    public IdentifierGenerationOptionTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.IDENTIFIERGENERATIONOPTIONTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return IdentifierGenerationOptionType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value) throws Exception {
        if ("UseExisting".equals(value)) {
            return IdentifierGenerationOptionType.USE_EXISTING_LITERAL;
        }

        if ("ReplaceDuplicate".equals(value)) {
            return IdentifierGenerationOptionType.REPLACE_DUPLICATE_LITERAL;
        }

        if ("GenerateNew".equals(value)) {
            return IdentifierGenerationOptionType.GENERATE_NEW_LITERAL;
        }

        return null;
    }
}
