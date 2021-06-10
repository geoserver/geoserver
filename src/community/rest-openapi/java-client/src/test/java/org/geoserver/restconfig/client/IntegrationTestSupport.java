package org.geoserver.restconfig.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.openapi.model.catalog.MetadataLinkInfo;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.model.ComponentVersion;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

/**
 * Junit {@link Rule} used to provide a pre-configured {@link GeoServerClient} and to enable
 * integration tests only if the geoserver instance is {@link #isAlive() accessible}
 *
 * <p>The {@code geoserver_port} environment variable needs to be provided
 */
@Slf4j
public class IntegrationTestSupport extends ExternalResource {

    private GeoServerClient client;

    private @NonNull String adminName = "admin";
    private @NonNull String adminPassword = "geoserver";

    private @Getter URL geoserverApiURL;

    private Set<String> workspacesToCleanup = new HashSet<>();

    protected @Override void before() throws IOException {
        final String apiUrl = resolve("geoserver_api_url", "http://localhost:18080/geoserver/rest");

        if (isEmpty(apiUrl)) {
            log.warn(
                    "No geoserver_api_url System property provided, can't run geoserver integration tests.");
            return;
        }
        this.geoserverApiURL = new URL(apiUrl);

        log.debug("Creating GeoSever API Client with baseURL {}", apiUrl);

        this.adminName = resolve("geoserver_admin_user", this.adminName);
        this.adminPassword = resolve("geoserver_admin_password", this.adminPassword);

        this.client = new GeoServerClient(apiUrl).setBasicAuth(this.adminName, this.adminPassword);
        this.client.logToStdErr();
        this.client.setDebugRequests(true);
    }

    protected @Override void after() {
        for (String ws : workspacesToCleanup) {
            try {
                client().workspaces().deleteRecursively(ws);
            } catch (Exception e) {
                log.error("Error deleting workspace {}", ws, e);
            }
        }
    }

    private String resolve(String variable) {
        return resolve(variable, null);
    }

    private String resolve(String variable, String defaultValue) {
        String val = System.getenv(variable);
        if (isEmpty(val)) {
            val = System.getProperty(variable);
        }
        if (isEmpty(val)) {
            val = defaultValue;
        }
        return val;
    }

    public GeoServerClient client() {
        return this.client;
    }

    public boolean isAlive() {
        if (null != this.client) {
            // make a workspaces query, we haven't implemented a status check
            // yet
            try {
                List<ComponentVersion> componentsVersion = this.client.getComponentsVersion();
                assertNotNull(componentsVersion);
                assertFalse(componentsVersion.isEmpty());
                return true;
            } catch (Exception e) {
                log.info(
                        String.format(
                                "GeoServer unavailable at URL %s: %s",
                                this.client.api().getBasePath(), e.getMessage()),
                        e);
            }
        }
        return false;
    }

    public URI getRoadsShapefile() {
        return URI.create("file:data/sf/roads.shp");
    }

    public URI getStreamsShapefile() {
        return URI.create("file:data/sf/streams.shp");
    }

    public URI getSFDemGeoTiff() {
        return URI.create("file:data/sf/sfdem.tif");
    }

    public WorkspaceInfo createWorkspace(@NonNull String name) {
        return createWorkspace(name, false);
    }

    public WorkspaceInfo createWorkspace(@NonNull String name, boolean isolated) {
        WorkspaceInfo requestBody = new WorkspaceInfo().name(name).isolated(isolated);
        WorkspacesClient workspaces = client().workspaces();
        workspaces.create(requestBody);
        WorkspaceInfo ws =
                workspaces
                        .getAsInfo(name)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "workspace " + name + " not created"));
        assertEquals(name, ws.getName());
        this.workspacesToCleanup.add(name);
        return ws;
    }

    static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public MetadataLinkInfo newMetadataLink(int sampleIndex) {
        return newMetadataLink(
                "Test link #" + sampleIndex,
                "http://test.com/mdlink#" + sampleIndex,
                "ISO19115:2003",
                "text/plain");
    }

    public MetadataLinkInfo newMetadataLink(
            String about, String contentURL, String metadataType, String contentType) {
        MetadataLinkInfo mdl = new MetadataLinkInfo();
        mdl.setAbout(about);
        mdl.setContent(contentURL);
        mdl.setMetadataType(metadataType);
        mdl.setType(contentType);
        return mdl;
    }
}
