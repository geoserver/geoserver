/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import java.util.ArrayList;
import java.util.List;

/** Utility class that provides the default Content Security Policy configuration. */
public final class CSPDefaultConfiguration {

    private CSPDefaultConfiguration() {}

    /** @return a new instance of the default configuration */
    public static CSPConfiguration newInstance() {
        List<CSPRule> rules1 = new ArrayList<>();
        rules1.add(new CSPRule(
                "static-html-files",
                "Allow unsafe scripts and remote resources on static HTML pages unless " + "disabled by a property.",
                true,
                "PATH(^/www/.*\\.html?$) "
                        + "AND PROP(GEOSERVER_DISABLE_STATIC_WEB_FILES,(?i)^(?!true$).*$) "
                        + "AND PROP(GEOSERVER_STATIC_WEB_FILES_SCRIPT,(?i)^(UNSAFE)?$)",
                "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                        + "connect-src 'self'; font-src 'self' ${geoserver.csp.remoteResources}; "
                        + "img-src 'self' ${geoserver.csp.remoteResources} data:; "
                        + "style-src 'self' ${geoserver.csp.remoteResources} 'unsafe-inline'; "
                        + "script-src 'self' ${geoserver.csp.remoteResources} 'unsafe-inline' 'unsafe-eval';"));
        rules1.add(new CSPRule(
                "ows-wms-featureinfo-html",
                "Allow unsafe scripts and remote resources on WMS GetFeatureInfo HTML "
                        + "output if enabled by a property.",
                true,
                "PATH(^/([^/]+/){0,2}ows/?$) "
                        + "AND PARAM((?i)^service$,(?i)^wms$) "
                        + "AND PARAM((?i)^request$,(?i)^getfeatureinfo$) "
                        + "AND PARAM((?i)^info_format$,(?i)^text/html$) "
                        + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                ""));
        rules1.add(new CSPRule(
                "wms-featureinfo-html",
                "Allow unsafe scripts and remote resources on WMS GetFeatureInfo HTML "
                        + "output if enabled by a property.",
                true,
                "PATH(^/([^/]+/){0,2}wms/?$) "
                        + "AND PARAM((?i)^service$,(?i)^(wms)?$) "
                        + "AND PARAM((?i)^request$,(?i)^getfeatureinfo$) "
                        + "AND PARAM((?i)^info_format$,(?i)^text/html$) "
                        + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                ""));
        rules1.add(new CSPRule(
                "wtms-kvp-featureinfo-html",
                "Allow unsafe scripts and remote resources on WMTS GetFeatureInfo HTML "
                        + "output if enabled by a property.",
                true,
                "PATH(^/([^/]+/){0,2}gwc/service/wmts/?$) "
                        + "AND PARAM((?i)^service$,(?i)^(wmts)?$) "
                        + "AND PARAM((?i)^request$,(?i)^getfeatureinfo$) "
                        + "AND PARAM((?i)^infoformat$,^text/html$) "
                        + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                ""));
        rules1.add(new CSPRule(
                "wtms-rest-featureinfo-html",
                "Allow unsafe scripts and remote resources on WMTS GetFeatureInfo HTML "
                        + "output if enabled by a property.",
                true,
                "PATH(^/([^/]+/){0,2}gwc/service/wmts/rest(/[^/]*){7,8}$) "
                        + "AND PARAM(^format$,^text/html$) "
                        + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                ""));
        rules1.add(new CSPRule(
                "index-page",
                "Allow unsafe scripts on the index.html page.",
                true,
                "PATH(^/index\\.html$)",
                "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                        + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                        + "style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline';"));
        rules1.add(new CSPRule(
                "other-requests",
                "Block unsafe scripts on all other requests.",
                true,
                "",
                "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                        + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                        + "style-src 'self' 'unsafe-inline'; script-src 'self';"));
        List<CSPRule> rules2 = new ArrayList<>();
        rules2.add(new CSPRule(
                "frame-ancestors-property",
                "Set frame-ancestors based on the CSP frame ancestors property or setting " + "when it is configured.",
                true,
                "PROP(geoserver.csp.frameAncestors,(?i)^[a-z0-9'\\*][a-z0-9_\\-':/\\.\\* ]{4,}$)",
                "frame-ancestors ${geoserver.csp.frameAncestors};"));
        rules2.add(new CSPRule(
                "frame-ancestors-self",
                "Pages can be displayed in frames with the same origin. This rule depends "
                        + "on the properties for the X-Frame-Options header.",
                true,
                "PROP(geoserver.xframe.shouldSetPolicy,(?i)^(true)?$) "
                        + "AND PROP(geoserver.xframe.policy,^(SAMEORIGIN)?$)",
                "frame-ancestors 'self';"));
        rules2.add(new CSPRule(
                "frame-ancestors-none",
                "Pages can not be displayed in any frames. This rule depends on the "
                        + "properties for the X-Frame-Options header.",
                true,
                "PROP(geoserver.xframe.shouldSetPolicy,(?i)^(true)?$) " + "AND PROP(geoserver.xframe.policy,^DENY$)",
                "frame-ancestors 'none';"));
        rules2.add(new CSPRule(
                "frame-ancestors-not-set",
                "Pages can be displayed in frames with any origin. This rule depends on "
                        + "the properties for the X-Frame-Options header.",
                true,
                "",
                "NONE"));
        List<CSPPolicy> policies = new ArrayList<>();
        policies.add(new CSPPolicy(
                "other-directives", "Rules to set the base-uri, form-action and fetch directives", true, rules1));
        policies.add(new CSPPolicy("frame-ancestors", "Rules to set the frame-ancestors directive", true, rules2));
        CSPConfiguration config = new CSPConfiguration();
        config.setPolicies(policies);
        return config.parseFilters();
    }
}
