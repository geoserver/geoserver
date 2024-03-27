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
        rules1.add(
                new CSPRule(
                        "wicket9",
                        "Wicket 9+ sets its own header that is missing the form-action directive "
                                + "and will be merged with this value. This rule can be disabled "
                                + "by removing or disabling the web interface. This rule may be "
                                + "updated after the Wicket 9+ upgrade.",
                        true,
                        "METHOD(GET,HEAD,POST) "
                                + "AND PATH(^/web/(wicket/page|wicket/bookmarkable/.*)?$) "
                                + "AND CLASS(org.geoserver.web.GeoServerApplication) "
                                + "AND CLASS(org.apache.wicket.csp.ContentSecurityPolicySettings) "
                                + "AND PROP(GEOSERVER_CONSOLE_DISABLED,(?i)^(?!true$).*$)",
                        "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                                + "connect-src 'self'; font-src 'self'; img-src 'self'; "
                                + "style-src 'self' 'unsafe-inline'; script-src 'self';"));
        rules1.add(
                new CSPRule(
                        "wicket8",
                        "Wicket 8 requires unsafe-inline and unsafe-eval. This rule can be "
                                + "disabled by removing or disabling the web interface. This rule "
                                + "will be removed after the Wicket 9+ upgrade.",
                        true,
                        "METHOD(GET,HEAD,POST) "
                                + "AND PATH(^/web/(wicket/page|wicket/bookmarkable/.*)?$) "
                                + "AND CLASS(org.geoserver.web.GeoServerApplication) "
                                + "AND PROP(GEOSERVER_CONSOLE_DISABLED,(?i)^(?!true$).*$)",
                        "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                                + "connect-src 'self'; font-src 'self'; img-src 'self'; "
                                + "style-src 'self' 'unsafe-inline'; "
                                + "script-src 'self' 'unsafe-inline' 'unsafe-eval';"));
        rules1.add(
                new CSPRule(
                        "static-web-files",
                        "Allow unsafe scripts in static web files by default and allow loading "
                                + "certain resources from an external server based on the "
                                + "configuration. This rule can be disabled by setting the "
                                + "property to disable all or only unsafe static web files.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/www/.*\\.html?$) "
                                + "AND PROP(GEOSERVER_DISABLE_STATIC_WEB_FILES,(?i)^(?!true$).*$) "
                                + "AND PROP(GEOSERVER_STATIC_WEB_FILES_SCRIPT,(?i)^(UNSAFE)?$)",
                        "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                                + "connect-src 'self'; font-src 'self' ${geoserver.csp.externalResources}; "
                                + "img-src 'self' ${geoserver.csp.externalResources} data:; "
                                + "style-src 'self' ${geoserver.csp.externalResources} 'unsafe-inline'; "
                                + "script-src 'self' ${geoserver.csp.externalResources} 'unsafe-inline' 'unsafe-eval';"));
        rules1.add(
                new CSPRule(
                        "ows-wms-featureinfo-html",
                        "Allow unsafe scripts in the WMS GetFeatureInfo HTML output format and "
                                + "allow loading certain resources from an external server based "
                                + "on the configuration. This rule will only be enabled when the "
                                + "system property is set to UNSAFE.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/([^/]+/){0,2}ows/?$) "
                                + "AND PARAM((?i)^service$,(?i)^wms$) "
                                + "AND PARAM((?i)^request$,(?i)^getfeatureinfo$) "
                                + "AND PARAM((?i)^info_format$,(?i)^text/html$) "
                                + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "wms-featureinfo-html",
                        "Allow unsafe scripts in the WMS GetFeatureInfo HTML output format and "
                                + "allow loading certain resources from an external server based "
                                + "on the configuration. This rule will only be enabled when the "
                                + "system property is set to UNSAFE.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/([^/]+/){0,2}wms/?$) "
                                + "AND PARAM((?i)^service$,(?i)^(wms)?$) "
                                + "AND PARAM((?i)^request$,(?i)^getfeatureinfo$) "
                                + "AND PARAM((?i)^info_format$,(?i)^text/html$) "
                                + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "wtms-kvp-featureinfo-html",
                        "Allow unsafe scripts in the WTMS KVP GetFeatureInfo HTML output format "
                                + "and allow loading certain resources from an external server "
                                + "based on the configuration. This rule will only be enabled "
                                + "when the system property is set to UNSAFE.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/([^/]+/){0,2}gwc/service/wmts/?$) "
                                + "AND PARAM((?i)^service$,(?i)^(wmts)?$) "
                                + "AND PARAM((?i)^request$,(?i)^getfeatureinfo$) "
                                + "AND PARAM((?i)^infoformat$,^text/html$) "
                                + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "wtms-rest-featureinfo-html",
                        "Allow unsafe scripts in the WTMS REST GetFeatureInfo HTML output format "
                                + "and allow loading certain resources from an external server "
                                + "based on the configuration. This rule will only be enabled "
                                + "when the system property is set to UNSAFE.",
                        true,
                        "METHOD(GET,HEAD) "
                                + "AND PATH(^/([^/]+/){0,2}gwc/service/wmts/rest(/[^/]*){7,8}$) "
                                + "AND PARAM(^format$,^text/html$) "
                                + "AND PROP(GEOSERVER_FEATUREINFO_HTML_SCRIPT,(?i)^UNSAFE$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "other-geoserver-inline",
                        "A few other GeoServer pages that use inline scripts.",
                        true,
                        "METHOD(GET,HEAD) "
                                + "AND PATH(^/(index\\.html|TestWfsPost|gwc/rest/seed/[^/]+/?)$) "
                                + "AND QUERY(^$)",
                        "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                                + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                                + "style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline';"));
        rules1.add(
                new CSPRule(
                        "gwc-openlayers",
                        "The GWC OpenLayers demo page uses inline scripts.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/gwc/demo/[^/]+/?$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "ows-wms-map-openlayers",
                        "The WMS GetMap OpenLayers output uses inline scripts.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/([^/]+/){0,2}ows/?$) "
                                + "AND PARAM((?i)^service$,(?i)^wms$) "
                                + "AND PARAM((?i)^request$,(?i)^getmap$) "
                                + "AND PARAM((?i)^format$,(?i)^(application/|text/html; subtype=)?openlayers[23]?$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "wms-map-openlayers",
                        "The WMS GetMap OpenLayers output uses inline scripts.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/([^/]+/){0,2}wms/?$) "
                                + "AND PARAM((?i)^service$,(?i)^(wms)?$) "
                                + "AND PARAM((?i)^request$,(?i)^getmap$) "
                                + "AND PARAM((?i)^format$,(?i)^(application/|text/html; subtype=)?openlayers[23]?$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "wms-reflect-openlayers",
                        "The WMS GetMap OpenLayers output uses inline scripts.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/([^/]+/){0,2}wms/reflect/?$) "
                                + "AND PARAM((?i)^service$,(?i)^(wms)?$) "
                                + "AND PARAM((?i)^request$,(?i)^(getmap)?$) "
                                + "AND PARAM((?i)^format$,(?i)^(application/|text/html; subtype=)?openlayers[23]?$)",
                        ""));
        rules1.add(
                new CSPRule(
                        "ogc-api",
                        "Temporarily allow inline scripts for all OGC API requests. This rule "
                                + "will only be enabled when the OGC API community modules are "
                                + "installed and may be fine-tuned in the future.",
                        true,
                        "METHOD(GET,HEAD) AND PATH(^/([^/]+/){0,2}ogc(/.*)?$) "
                                + "AND CLASS(org.geoserver.ogcapi.APIDispatcher)",
                        ""));
        rules1.add(
                new CSPRule(
                        "other-requests",
                        "Disable inline scripts for all other requests.",
                        true,
                        "",
                        "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                                + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                                + "style-src 'self' 'unsafe-inline'; script-src 'self';"));
        List<CSPRule> rules2 = new ArrayList<>();
        rules2.add(
                new CSPRule(
                        "frame-ancestors-property",
                        "Set frame-ancestors based on a property. This rule will be enabled when "
                                + "the CSP frame ancestors property or setting is configured.",
                        true,
                        "PROP(geoserver.csp.frameAncestors,(?i)^[a-z0-9'\\*][a-z0-9_\\-':/\\.\\* ]{4,}$)",
                        "frame-ancestors ${geoserver.csp.frameAncestors};"));
        rules2.add(
                new CSPRule(
                        "frame-ancestors-self",
                        "Pages can be displayed in frames with the same origin. This rule depends "
                                + "on the properties for the X-Frame-Options header.",
                        true,
                        "PROP(geoserver.xframe.shouldSetPolicy,(?i)^(true)?$) "
                                + "AND PROP(geoserver.xframe.policy,^(SAMEORIGIN)?$)",
                        "frame-ancestors 'self';"));
        rules2.add(
                new CSPRule(
                        "frame-ancestors-none",
                        "Pages can not be displayed in any frames. This rule depends on the "
                                + "properties for the X-Frame-Options header.",
                        true,
                        "PROP(geoserver.xframe.shouldSetPolicy,(?i)^(true)?$) "
                                + "AND PROP(geoserver.xframe.policy,^DENY$)",
                        "frame-ancestors 'none';"));
        rules2.add(
                new CSPRule(
                        "frame-ancestors-not-set",
                        "Pages can be displayed in frames with any origin. This rule depends on "
                                + "the properties for the X-Frame-Options header.",
                        true,
                        "",
                        "NONE"));
        List<CSPPolicy> policies = new ArrayList<>();
        policies.add(
                new CSPPolicy(
                        "other-directives",
                        "Rules to set the base-uri, form-action and fetch directives",
                        true,
                        rules1));
        policies.add(
                new CSPPolicy(
                        "frame-ancestors",
                        "Rules to set the frame-ancestors directive",
                        true,
                        rules2));
        return new CSPConfiguration(true, false, "", "", policies).parseFilters();
    }
}
