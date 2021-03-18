package org.geoserver.restconfig.client;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.openapi.model.config.WCSInfo;
import org.geoserver.openapi.model.config.WFSInfo;
import org.geoserver.openapi.model.config.WMSInfo;
import org.geoserver.openapi.model.config.WMTSInfo;
import org.geoserver.openapi.v1.client.OwsServicesApi;
import org.geoserver.openapi.v1.model.WCSInfoWrapper;
import org.geoserver.openapi.v1.model.WFSInfoWrapper;
import org.geoserver.openapi.v1.model.WMSInfoWrapper;
import org.geoserver.openapi.v1.model.WMTSInfoWrapper;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class OwsServicesClient {

    private @NonNull GeoServerClient client;

    public OwsServicesApi api() {
        return client.api(OwsServicesApi.class);
    }

    /** Retrieves Web Coverage Service settings globally for the server. */
    public WCSInfo getWCSSettings() {
        return api().getWCSSettings().getWcs();
    }

    /** Retrieves Web Feature Service settings globally for the server. */
    public WFSInfo getWFSSettings() {
        return api().getWFSSettings().getWfs();
    }

    /** Retrieves Web Map Service settings globally for the server. */
    public WMSInfo getWMSSettings() {
        return api().getWMSSettings().getWms();
    }

    /**
     * Retrieves Web Coverage Service settings for a given workspace.
     *
     * @param workspace The workspace name (required)
     */
    public WCSInfo getLocalWCSSettings(@NonNull String workspace) {
        return api().getLocalWCSSettings(workspace).getWcs();
    }

    /**
     * Retrieves Web Feature Service settings for a given workspace.
     *
     * @param workspace The workspace name (required)
     */
    public WFSInfo getLocalWFSSettings(@NonNull String workspace) {
        return api().getLocalWFSSettings(workspace).getWfs();
    }

    /**
     * Retrieves Web Map Service settings for a given workspace.
     *
     * @param workspace The workspace name (required)
     */
    public WMSInfo getLocalWMSSettings(@NonNull String workspace) {
        return api().getLocalWMSSettings(workspace).getWms();
    }

    /**
     * Deletes a workspace-specific WCS setting.
     *
     * @param workspace The workspace name (required)
     */
    public void deleteLocalWCSSettings(@NonNull String workspace) {
        api().deleteLocalWCSSettings(workspace);
    }

    /**
     * Deletes a workspace-specific WFS setting.
     *
     * @param workspace The workspace name (required)
     */
    public void deleteLocalWFSSettings(@NonNull String workspace) {
        api().deleteLocalWFSSettings(workspace);
    }

    /**
     * Deletes a workspace-specific WMS setting.
     *
     * @param workspace The workspace name (required)
     */
    public void deleteLocalWMSSettings(@NonNull String workspace) {
        api().deleteLocalWMSSettings(workspace);
    }

    /**
     * Edits a workspace-specific WCS setting.
     *
     * @param workspace The workspace name (required)
     * @param info Body of the WCS settings (required)
     */
    public void updateLocalWCSSettings(@NonNull String workspace, @NonNull WCSInfo info) {
        api().updateLocalWCSSettings(workspace, new WCSInfoWrapper().wcs(info));
    }

    /**
     * Edits a workspace-specific WFS setting.
     *
     * @param workspace The workspace name (required)
     * @param info Body of the WFS settings (required)
     */
    public void updateLocalWFSSettings(@NonNull String workspace, @NonNull WFSInfo info) {
        api().updateLocalWFSSettings(workspace, new WFSInfoWrapper().wfs(info));
    }

    /**
     * Edits a workspace-specific WMS setting.
     *
     * @param workspace The workspace name (required)
     * @param info Body of the WMS settings (required)
     */
    public void updateLocalWMSSettings(@NonNull String workspace, @NonNull WMSInfo info) {
        api().updateLocalWMSSettings(workspace, new WMSInfoWrapper().wms(info));
    }

    /**
     * Edits a global WCS setting.
     *
     * @param wcs Body of the WCS settings (required)
     */
    public void updateWCSSettings(@NonNull WCSInfo wcs) {
        api().updateWCSSettings(new WCSInfoWrapper().wcs(wcs));
    }

    /**
     * Edits a global WFS setting.
     *
     * @param wfs Body of the WFS settings (required)
     */
    public void updateWFSSettings(@NonNull WFSInfo wfs) {
        api().updateWFSSettings(new WFSInfoWrapper().wfs(wfs));
    }

    /**
     * Edits a global WMS setting.
     *
     * @param wms Body of the WMS settings (required)
     */
    public void updateWMSSettings(@NonNull WMSInfo wms) {
        api().updateWMSSettings(new WMSInfoWrapper().wms(wms));
    }

    /** Retrieves Web Map Tile Service global settings.. */
    public WMTSInfo getWMTSSettings() {
        return api().getWMTSSettings().getWmts();
    }

    /** Edits a global WMTS setting. */
    public void updateWMTSSettings(@NonNull WMTSInfo wmts) {
        api().updateWMTSSettings(new WMTSInfoWrapper().wmts(wmts));
    }

    /** Deletes a workspace-specific WMTS setting. */
    public void deleteLocalWMTSSettings(@NonNull String workspace) {
        api().deleteLocalWMTSSettings(workspace);
    }

    /** Retrieves Web Map Service settings for a given workspace. */
    public WMTSInfo getLocalWMTSSettings(@NonNull String workspace) {
        return api().getLocalWMTSSettings(workspace).getWmts();
    }

    /** Edits a workspace-specific WMTS setting. */
    public void updateLocalWMTSSettings(@NonNull String workspace, @NonNull WMTSInfo wmts) {
        api().updateLocalWMTSSettings(workspace, new WMTSInfoWrapper().wmts(wmts));
    }
}
