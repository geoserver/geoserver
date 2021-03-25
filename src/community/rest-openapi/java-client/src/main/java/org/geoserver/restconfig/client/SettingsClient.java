package org.geoserver.restconfig.client;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.openapi.model.config.ContactInfo;
import org.geoserver.openapi.model.config.GeoServerInfo;
import org.geoserver.openapi.model.config.SettingsInfo;
import org.geoserver.openapi.v1.client.SettingsApi;
import org.geoserver.openapi.v1.model.ContactInfoWrapper;
import org.geoserver.openapi.v1.model.GeoServerInfoWrapper;
import org.geoserver.openapi.v1.model.SettingsInfoWrapper;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SettingsClient {

    private @NonNull GeoServerClient client;

    public SettingsApi api() {
        return client.api(SettingsApi.class);
    }

    /**
     * Get geoserver global settings
     *
     * @return GeoServerInfoWrapper
     */
    public GeoServerInfo getGlobalConfig() {
        GeoServerInfoWrapper response = api().getSettings();
        GeoServerInfo global = response.getGlobal();
        return global;
    }

    /**
     * Update the global configuration Adds a new data store to the workspace.
     *
     * @param global global settings upload request body (required)
     */
    public GeoServerInfo updateGlobalConfig(GeoServerInfo global) {
        api().updateSettings(new GeoServerInfoWrapper().global(global));
        return getGlobalConfig();
    }

    /**
     * Create workspace-specific settings Create new workspace-specific settings on the server.
     *
     * @param workspace The workspace name (required)
     * @param settings The workspace-specific settings information to upload. (required)
     */
    public void createLocalSettings(@NonNull String workspace, SettingsInfo settings) {
        api().createLocalSettings(workspace, new SettingsInfoWrapper().settings(settings));
    }

    /**
     * Get the global contact information Displays a list of all global contact settings on the
     * server. This is a subset of what is available at the /settings endpoint.
     */
    public ContactInfo getGlobalContactInfo() {
        return api().getGlobalContactInfo().getContact();
    }

    /**
     * Get workspace-specific settings Displays all workspace-specific settings.
     *
     * @param workspace The workspace name (required)
     * @return SettingsInfoWrapper
     */
    public SettingsInfo getLocalSettings(@NonNull String workspace) {
        return api().getLocalSettings(workspace).getSettings();
    }

    /**
     * Update contact settings Updates global contact settings on the server.
     *
     * @param contactInfo The contact settings information to upload. (required)
     */
    public void updateGlobalContactInfo(@NonNull ContactInfo contactInfo) {
        api().updateGlobalContactInfo(new ContactInfoWrapper().contact(contactInfo));
    }

    /**
     * Update workspace-specific settings Updates workspace-specific settings on the server.
     *
     * @param workspace The workspace name (required)
     * @param settings The workspace-specific settings information to upload. (required)
     */
    public void updateLocalSettings(@NonNull String workspace, SettingsInfo settings) {
        api().updateLocalSettings(workspace, new SettingsInfoWrapper().settings(settings));
    }
}
