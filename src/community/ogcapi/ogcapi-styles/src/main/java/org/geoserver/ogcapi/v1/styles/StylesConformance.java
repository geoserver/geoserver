/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static org.geoserver.ogcapi.APIConformance.Level.DRAFT_STANDARD;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.platform.GeoServerExtensions;

/** Conformance classes of the OGC API - Styles service. */
public class StylesConformance extends ConformanceInfo<StylesServiceInfo> {

    /**
     * Enables {@link #MANAGE_STYLES} when set to true, as a system property, environment variable or context parameter.
     */
    public static final String MANAGE_STYLES_PROPERTY = "OGCAPI_STYLES_MANAGE";

    public static final APIConformance CORE = new APIConformance(StylesService.CORE, DRAFT_STANDARD);
    public static final APIConformance HTML = CORE.extend(StylesService.HTML, DRAFT_STANDARD);
    public static final APIConformance JSON = CORE.extend(StylesService.JSON, DRAFT_STANDARD);
    public static final APIConformance MAPBOX = CORE.extend(StylesService.MAPBOX, DRAFT_STANDARD);
    public static final APIConformance SLD10 = CORE.extend(StylesService.SLD10, DRAFT_STANDARD);
    public static final APIConformance SLD11 = CORE.extend(StylesService.SLD11, DRAFT_STANDARD);

    public static final APIConformance MANAGE_STYLES = new APIConformance(
            StylesService.MANAGE, DRAFT_STANDARD, APIConformance.Type.EXTENSION, CORE, "manageStyles");

    @Override
    public String getId() {
        return "styles";
    }

    @Override
    public List<APIConformance> configurableConformances() {
        return new ArrayList<>();
    }

    @Override
    public List<APIConformance> conformances(StylesServiceInfo info) {
        List<APIConformance> conformance = new ArrayList<>();
        if (!isEnabled(info)) return conformance;
        conformance.addAll(List.of(CORE, HTML, JSON, MAPBOX, SLD10, SLD11));
        if (manageStyles()) conformance.add(MANAGE_STYLES);
        return conformance;
    }

    /** Returns true if styles can be created, updated and deleted through the API. */
    public static boolean manageStyles() {
        return Boolean.parseBoolean(GeoServerExtensions.getProperty(MANAGE_STYLES_PROPERTY));
    }
}
