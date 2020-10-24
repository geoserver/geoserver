/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.dggs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.Link;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.util.ResponseUtils;

public class CollectionDAPA extends AbstractDocument {

    private static final String ENDPOINT_REL = "ogc-dapa-endpoint";
    private static final List<String> AGGREGATION_MEDIA_TYPES =
            Arrays.asList("application/geo+json", "application/dggs+json" /*, "text/csv" */);
    String title = "Data retrieval patterns";
    String description;
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
        addSelfLinks("ogc/dggs/collections/" + collectionId + "/dapa/variables");

        // aggregations
        addAreaEndpoint(collectionId);
        addAreaSpaceEndpoint(collectionId);
        addAreaTimeEndpoint(collectionId);
        addAreaSpaceTimeEndpoint(collectionId);
    }

    private void addAreaEndpoint(String collectionId) {
        DAPAEndpoint endpoint = new DAPAEndpoint("area", collectionId);
        endpoint.setTitle(
                "This DAPA endpoint returns observation values at the selected location (parameter coord or coordRef) in the selected time interval or at the selected time instant (parameter datetime).");
        endpoint.setDescription(
                "This DAPA endpoint returns "
                        + collectionId
                        + " values for an area (parameter `bbox`, `geom` or `zones`) in the selected time interval or at the selected time instant (parameter `datetime`). \nNo aggregation is performed, this just returns the data as-is.");
        Link executLink = new Link();
        executLink.setRel(ENDPOINT_REL);
        executLink.setClassification(ENDPOINT_REL);
        executLink.setType("application/geo+json");
        executLink.setTitle("Execute the data retrieval pattern with the default parameters");
        String path =
                ResponseUtils.appendPath(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/dggs/collections",
                        collectionId,
                        "dapa/area");
        executLink.setHref(path);
        endpoint.addLink(executLink);
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
        Link executLink = new Link();
        executLink.setRel(ENDPOINT_REL);
        executLink.setClassification(ENDPOINT_REL);
        executLink.setType("application/geo+json");
        executLink.setTitle("Execute the data retrieval pattern with the default parameters");
        String path =
                ResponseUtils.appendPath(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/dggs/collections",
                        collectionId,
                        "dapa/area:aggregate-space");
        executLink.setHref(path);
        endpoint.addLink(executLink);
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
        Link executLink = new Link();
        executLink.setRel(ENDPOINT_REL);
        executLink.setClassification(ENDPOINT_REL);
        executLink.setType("application/geo+json");
        executLink.setTitle("Execute the data retrieval pattern with the default parameters");
        String path =
                ResponseUtils.appendPath(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/dggs/collections",
                        collectionId,
                        "dapa/area:aggregate-time");
        executLink.setHref(path);
        endpoint.addLink(executLink);
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
        Link executLink = new Link();
        executLink.setRel(ENDPOINT_REL);
        executLink.setClassification(ENDPOINT_REL);
        executLink.setType("application/geo+json");
        executLink.setTitle("Execute the data retrieval pattern with the default parameters");
        String path =
                ResponseUtils.appendPath(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/dggs/collections",
                        collectionId,
                        "dapa/area:aggregate-space-time");
        executLink.setHref(path);
        endpoint.addLink(executLink);
        endpoint.setMediaTypes(AGGREGATION_MEDIA_TYPES);

        endpoints.add(endpoint);
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
