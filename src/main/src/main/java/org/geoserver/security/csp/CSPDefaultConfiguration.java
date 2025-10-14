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
                "Allow unsafe scripts and remote resources on static HTML pages unless disabled by a property.",
                true,
                "PATH(^/www/.*\\.html?$) "
                        + "AND PROP(GEOSERVER_DISABLE_STATIC_WEB_FILES,(?i)^(?!true$).*$) "
                        + "AND PROP(GEOSERVER_STATIC_WEB_FILES_SCRIPT,(?i)^(UNSAFE)?$)",
                "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                        + "font-src 'self' ${geoserver.csp.remoteResources}; "
                        + "img-src 'self' ${geoserver.csp.remoteResources} data:; "
                        + "style-src 'self' ${geoserver.csp.remoteResources} 'unsafe-inline'; "
                        + "script-src 'self' ${geoserver.csp.remoteResources} 'unsafe-inline' 'unsafe-eval'; "
                        + "form-action ${geoserver.csp.formAction}; "
                        + "frame-ancestors ${geoserver.csp.frameAncestors};"));
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
                "base-uri 'self'; default-src 'none'; child-src 'self'; "
                        + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                        + "style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; "
                        + "form-action ${geoserver.csp.formAction}; "
                        + "frame-ancestors ${geoserver.csp.frameAncestors};"));
        rules1.add(new CSPRule(
                "other-requests",
                "Block unsafe scripts on all other requests.",
                true,
                "",
                "base-uri 'self'; default-src 'none'; child-src 'self'; "
                        + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                        + "style-src 'self' 'unsafe-inline'; script-src 'self'; "
                        + "form-action ${geoserver.csp.formAction}; "
                        + "frame-ancestors ${geoserver.csp.frameAncestors};"));
        List<CSPPolicy> policies = new ArrayList<>();
        policies.add(new CSPPolicy(
                "geoserver-csp", "Rules to set GeoServer's Content-Security-Policy header", true, rules1));
        CSPConfiguration config = new CSPConfiguration();
        config.setPolicies(policies);
        return config.parseFilters();
    }
}
