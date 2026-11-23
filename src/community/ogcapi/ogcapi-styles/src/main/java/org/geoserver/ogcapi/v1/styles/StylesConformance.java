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

/** Conformance class configuration for the OGC API - Styles service, stored in the {@link StylesServiceInfo}. */
public class StylesConformance extends ConformanceInfo<StylesServiceInfo> {

    public static final String METADATA_KEY = "ogcapiStyles";

    public static final APIConformance CORE = new APIConformance(StylesService.CORE, DRAFT_STANDARD);
    public static final APIConformance HTML = CORE.extend(StylesService.HTML, DRAFT_STANDARD);
    public static final APIConformance JSON = CORE.extend(StylesService.JSON, DRAFT_STANDARD);
    public static final APIConformance MAPBOX = CORE.extend(StylesService.MAPBOX, DRAFT_STANDARD);
    public static final APIConformance SLD10 = CORE.extend(StylesService.SLD10, DRAFT_STANDARD);
    public static final APIConformance SLD11 = CORE.extend(StylesService.SLD11, DRAFT_STANDARD);

    public static final APIConformance MANAGE_STYLES = new APIConformance(
            StylesService.MANAGE, DRAFT_STANDARD, APIConformance.Type.EXTENSION, CORE, "manageStyles");

    private Boolean manageStyles;

    /** Returns the configuration stored in the service, or a default one, without modifying the service. */
    public static StylesConformance configuration(StylesServiceInfo info) {
        StylesConformance conformance = info.getMetadata().get(METADATA_KEY, StylesConformance.class);
        return conformance != null ? conformance : new StylesConformance();
    }

    @Override
    public String getId() {
        return "styles";
    }

    @Override
    public List<APIConformance> configurableConformances() {
        return new ArrayList<>(List.of(MANAGE_STYLES));
    }

    @Override
    public List<APIConformance> conformances(StylesServiceInfo info) {
        List<APIConformance> conformance = new ArrayList<>();
        if (!isEnabled(info)) return conformance;
        conformance.addAll(List.of(CORE, HTML, JSON, MAPBOX, SLD10, SLD11));
        if (manageStyles(info)) conformance.add(MANAGE_STYLES);
        return conformance;
    }

    /** Style management changes the catalog, so it is enabled only by explicit configuration. */
    @Override
    protected boolean isEnabled(StylesServiceInfo info, Boolean enabled, APIConformance conformance) {
        if (enabled == null && MANAGE_STYLES.equals(conformance)) return false;
        return super.isEnabled(info, enabled, conformance);
    }

    /** Returns true if styles can be created, updated and deleted through the API. */
    public boolean manageStyles(StylesServiceInfo info) {
        return isEnabled(info, manageStyles, MANAGE_STYLES);
    }

    public Boolean getManageStyles() {
        return manageStyles;
    }

    public void setManageStyles(Boolean manageStyles) {
        this.manageStyles = manageStyles;
    }
}
