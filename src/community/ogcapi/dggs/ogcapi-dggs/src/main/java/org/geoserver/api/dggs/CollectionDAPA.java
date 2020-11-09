/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.dggs;

import static org.geoserver.ows.URLMangler.URLType.SERVICE;
import static org.geoserver.ows.util.ResponseUtils.appendPath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.Link;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.data.Query;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.feature.visitor.MinVisitor;

public class CollectionDAPA extends AbstractDocument {

    private static final String ENDPOINT_REL = "ogc-dapa-endpoint";
    private static final List<String> AGGREGATION_MEDIA_TYPES =
            Arrays.asList("application/geo+json", "application/dggs+json" /*, "text/csv" */);
    String title = "Data retrieval patterns";
    String description;
    int minResolution;
    List<DAPAEndpoint> endpoints = new ArrayList<>();
    DAPAVariables variables;
    List<String> functions = new ArrayList<>(AggregateConverter.getAggregates().keySet());

    public CollectionDAPA(String collectionId, FeatureTypeInfo info) throws IOException {
        this.id = collectionId;
        this.description =
                "The following endpoints are available to retrieve and process the "
                        + collectionId
                        + " zones in addition to the standard DGGS queries.\n The endpoints are described in the API definition and the links point to the specification of the operation in the OpenAPI definition with the available input parameters and the response schema";
        this.variables = new DAPAVariables(collectionId, info);
        this.variables.getLinks().clear();
        this.minResolution = getMinResolution(info);
        addSelfLinks("ogc/dggs/collections/" + collectionId + "/dapa/variables");

        // aggregations
        addAreaEndpoint(collectionId);
        addAreaSpaceEndpoint(collectionId);
        addAreaTimeEndpoint(collectionId);
        addAreaSpaceTimeEndpoint(collectionId);
        addPositionEndpoint(collectionId);
        addPositionTimeEndpoint(collectionId);
    }

    private int getMinResolution(FeatureTypeInfo info) throws IOException {
        MinVisitor visitor = new MinVisitor(DGGSStore.RESOLUTION);
        info.getFeatureSource(null, null).getFeatures(Query.ALL).accepts(visitor, null);
        return visitor.getResult().toInt();
    }

    private void addAreaEndpoint(String collectionId) {
        DAPAEndpoint endpoint = new DAPAEndpoint("area", collectionId);
        endpoint.setTitle(
                "This DAPA endpoint returns observation values at the selected location (parameter coord or coordRef) in the selected time interval or at the selected time instant (parameter datetime).");
        endpoint.setDescription(
                "This DAPA endpoint returns "
                        + collectionId
                        + " values for an area (parameter `bbox`, `geom` or `zones`) in the selected time interval or at the selected time instant (parameter `datetime`). \nNo aggregation is performed, this just returns the data as-is.");
        Link executeLink = getExecuteLink(collectionId, "dapa/area");
        endpoint.addLink(executeLink);
        endpoint.setMediaTypes(Arrays.asList("application/geo+json", "application/dggs+json"));

        endpoints.add(endpoint);
    }

    private void addAreaSpaceEndpoint(String collectionId) {
        DAPAEndpoint endpoint = new DAPAEndpoint("area:aggregate-space", collectionId);
        endpoint.setTitle(
                "Retrieve a time series for selected variables for each station in an area and apply functions on the values of each time step");
        endpoint.setDescription(
                "This DAPA endpoint returns a time series for an area in the selected time inver"
                        + collectionId
                        + " values for an area (parameter `bbox`, `geom` or `zones`) in the selected time interval or at the selected time instant `datetime`). \nAll values in the area for each requested variable (parameter `variables`) are aggregated for each time step and each of the requested statistical functions (parameter `functions`) is applied to the aggregated values");
        Link executeLink = getExecuteLink(collectionId, "dapa/area:aggregate-space");
        endpoint.addLink(executeLink);
        endpoint.setMediaTypes(AGGREGATION_MEDIA_TYPES);

        endpoints.add(endpoint);
    }

    private void addAreaTimeEndpoint(String collectionId) {
        DAPAEndpoint endpoint = new DAPAEndpoint("area:aggregate-time", collectionId);
        endpoint.setTitle(
                "Retrieve a time series for selected variables for each zone in an area and apply functions on the values of each time series.");
        endpoint.setDescription(
                "This DAPA endpoint returns a time aggregate for each zone in an area, in the selected time interval.\n"
                        + "Each result contains contains the aggregation functions evaluated over the time series of each value associated to the zone.");
        Link executeLink = getExecuteLink(collectionId, "dapa/area:aggregate-time");
        endpoint.addLink(executeLink);
        endpoint.setMediaTypes(AGGREGATION_MEDIA_TYPES);

        endpoints.add(endpoint);
    }

    private void addAreaSpaceTimeEndpoint(String collectionId) {
        DAPAEndpoint endpoint = new DAPAEndpoint("area:aggregate-space-time", collectionId);
        endpoint.setTitle(
                "Retrieve a time series for selected variables for each station in an area and apply functions on all values");
        endpoint.setDescription(
                "This DAPA endpoint returns "
                        + collectionId
                        + " values for an area (parameter `bbox`, `geom` or `zones`) in the selected time interval or at the selected time instant (parameter `datetime`). \nAll values for each requested variable (parameter `variables`) are aggregated and each of the requested statistical functions (parameter `functions`) is applied to the aggregated values.");
        Link executeLink = getExecuteLink(collectionId, "dapa/area:aggregate-space-time");
        endpoint.addLink(executeLink);
        endpoint.setMediaTypes(AGGREGATION_MEDIA_TYPES);

        endpoints.add(endpoint);
    }

    private void addPositionEndpoint(String collectionId) {
        DAPAEndpoint endpoint = new DAPAEndpoint("position", collectionId);
        endpoint.setTitle(
                "This DAPA endpoint returns a time series in the selected zone (or point), in the selected time interval or at the selected time instant (parameter datetime).");
        endpoint.setDescription(
                "The time series contains values for each selected variable (parameter variables) for which a value can be interpolated at the location.");
        Link executeLink = getExecuteLink(collectionId, "dapa/position");
        endpoint.addLink(executeLink);
        endpoint.setMediaTypes(Arrays.asList("application/geo+json", "application/dggs+json"));

        endpoints.add(endpoint);
    }

    private void addPositionTimeEndpoint(String collectionId) {
        DAPAEndpoint endpoint = new DAPAEndpoint("position:aggregate-time", collectionId);
        endpoint.setTitle(
                "This DAPA endpoint returns values at the selected zone (parameter geom or zone_id) in the selected time interval or at the selected time instant (parameter datetime).");
        endpoint.setDescription(
                "All values in the time interval for each requested variable (parameter variables) are aggregated and each of the requested statistical functions (parameter functions) is applied to the aggregated values.");
        Link executeLink = getExecuteLink(collectionId, "dapa/position:aggregate-time");
        endpoint.addLink(executeLink);
        endpoint.setMediaTypes(AGGREGATION_MEDIA_TYPES);

        endpoints.add(endpoint);
    }

    private Link getExecuteLink(String collectionId, String operation) {
        String baseURL = APIRequestInfo.get().getBaseURL();
        String path = appendPath("ogc/dggs/collections", collectionId, operation);
        Map<String, String> kvp = new LinkedHashMap<>();
        kvp.put("resolution", String.valueOf(minResolution));
        // TODO: let is use HTML if/when an HTML representation for the DAPA resources is produced
        // by not setting the format. Right now it's necessary otherwise GML tries to run the
        // encoding
        kvp.put("f", "application/geo+json");
        String href = ResponseUtils.buildURL(baseURL, path, kvp, SERVICE);

        Link executeLink = new Link();
        executeLink.setRel(ENDPOINT_REL);
        executeLink.setClassification(ENDPOINT_REL);
        executeLink.setType("application/geo+json");
        executeLink.setTitle("Execute the data retrieval pattern with the default parameters");
        executeLink.setHref(href);
        return executeLink;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<DAPAEndpoint> getEndpoints() {
        return endpoints;
    }

    public DAPAVariables getVariables() {
        return variables;
    }

    public List<String> getFunctions() {
        return functions;
    }
}
