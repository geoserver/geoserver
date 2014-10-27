/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.CatalogValidator;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import java.util.regex.Pattern;

/**
 * Ensure that Catalog objects in GeoServer have names that can safely be used
 * in XML output.
 *
 * XML specification info for this code comes from http://www.w3.org/TR/REC-xml/
 * 
 * @author David Winslow, Boundless
 */

public class XMLSafeNameValidator implements CatalogValidator {
    private static Pattern XML_NAME_PATTERN;

    static {
        // Definitions coming from
        // NameStartChar ::= ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] |
        // [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] |
        // [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
        // NameChar ::= NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
        // Name ::= NameStartChar (NameChar)*
        String nameStartCharSet = "A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F"
                + "\u1FFF\u200C\u200D\u2070-\u218F\u2C00\u2FEF\u3001\uD7FF\uF900-\uFDCF"
                + "\uFDF0-\uFFFD";
        String nameStartChar = "[" + nameStartCharSet + "]";
        String nameChar = ("[" + nameStartCharSet + "\\-.0-9\u0087\u0300-\u036F\u203F-\u2040]");
        String name = "(?:" + nameStartChar + nameChar + "*)";
        XML_NAME_PATTERN = Pattern.compile(name, Pattern.CASE_INSENSITIVE);

    }

    private boolean safeName(String name) {
        return XML_NAME_PATTERN.matcher(name).matches();
    }

    public void validate(ResourceInfo resource, boolean isNew) {
        if (!safeName(resource.getName())) throw new RuntimeException("Name '" + resource.getName() + "' is not legal XML'");
    }

    public void validate(StoreInfo store, boolean isNew) {
        if (!safeName(store.getName())) throw new RuntimeException("Name '" + store.getName() + "' is not legal XML'");
    }

    public void validate(WorkspaceInfo workspace, boolean isNew) {
        if (!safeName(workspace.getName())) throw new RuntimeException("Name '" + workspace.getName() + "' is not legal XML'");
    }

    public void validate(LayerInfo layer, boolean isNew) {
        if (!safeName(layer.getName())) throw new RuntimeException("Name '" + layer.getName() + "' is not legal XML'");
    }

    public void validate(StyleInfo style, boolean isNew) {
        if (!safeName(style.getName())) throw new RuntimeException("Name '" + style.getName() + "' is not legal XML'");
    }

    public void validate(LayerGroupInfo layerGroup, boolean isNew) {
        if (!safeName(layerGroup.getName())) throw new RuntimeException("Name '" + layerGroup.getName() + "' is not legal XML'");
    }

    public void validate(NamespaceInfo namespace, boolean isNew) {
        if (!safeName(namespace.getName())) throw new RuntimeException("Name '" + namespace.getName() + "' is not legal XML'");
    }
}
