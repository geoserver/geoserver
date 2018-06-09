/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.ExtensionType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs2_0.WCS20Const;

/**
 * Parses overview policy from KVP
 *
 * @author Daniele Romagnoli - GeoSolutions
 */
public class OverviewPolicyKvpParser extends KvpParser {

    public OverviewPolicyKvpParser() {
        super(WCS20Const.OVERVIEW_POLICY_EXTENSION, ExtensionType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        // clean up extra space
        value = value.trim();

        ExtensionItemType se = Wcs20Factory.eINSTANCE.createExtensionItemType();
        se.setName(WCS20Const.OVERVIEW_POLICY_EXTENSION);
        se.setNamespace(WCS20Const.OVERVIEW_POLICY_EXTENSION_NAMESPACE);
        se.setSimpleContent(value);
        return se;
    }
}
