/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.web.api.RESTUtils.getStringAttribute;
import static org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

/**
 * Utility for handling GeoGIG repository init requests. This class will pull repository creation
 * details (like parent directory, or PostgreSQL database connection parameters) from the Request
 * and build a GeoGIG repository form them, by converting the request into a
 * {@link org.locationtech.geogig.repository.Hints Hints}.
 */
class InitRequestHandler {

    private static final InitRequestHandler INSTANCE = new InitRequestHandler();

    static final String REPO_ATTR = "repository";

    // Form parameter names
    /**
     * Directory option for Parent Directory.
     */
    static final String DIR_PARENT_DIR = "parentDirectory";
    /**
     * Database option for Host.
     */
    static final String DB_HOST = "dbHost";
    /**
     * Database option for Port.
     */
    static final String DB_PORT = "dbPort";
    /**
     * Database option for database name.
     */
    static final String DB_NAME = "dbName";
    /**
     * Database option for schema name.
     */
    static final String DB_SCHEMA = "dbSchema";
    /**
     * Database option for username.
     */
    static final String DB_USER = "dbUser";
    /**
     * Database option for password.
     */
    static final String DB_PASSWORD = "dbPassword";
    /**
     * GeoGig author name.
     */
    static final String AUTHOR_NAME = "authorName";
    /**
     * GeoGig author email.
     */
    static final String AUTHOR_EMAIL = "authorEmail";

    private void addParameter(Map<String, String> params, String key, String value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    private void updateRequestWithAuthor(Request request, Map<String, String> params) {
        // store author name and email in the attributes, if they were provided
        if (params.containsKey(AUTHOR_NAME)) {
            request.getAttributes().put(AUTHOR_NAME, params.get(AUTHOR_NAME));
        }
        if (params.containsKey(AUTHOR_EMAIL)) {
            request.getAttributes().put(AUTHOR_EMAIL, params.get(AUTHOR_EMAIL));
        }
    }

    /**
     * Adds JSON request parameters to the parameter Map. Look for known parameters in the JSON
     * object and populate the supplied map with requested values. This method does the same thing as
     * the {@link #addParameters(java.util.Map, org.restlet.data.Form)} version, except this one is
     * for Requests with a JSON payload, as opposed to a URL encoded form.
     *
     * @param params Map to hold request parameters.
     * @param json   JSONObject from a Request with parameters in a JSON payload.
     */
    private void addParameters(Map<String, String> params, JSONObject json) {
        addParameter(params, DIR_PARENT_DIR, json.optString(DIR_PARENT_DIR, null));
        addParameter(params, DB_HOST, json.optString(DB_HOST, null));
        addParameter(params, DB_PORT, json.optString(DB_PORT, null));
        addParameter(params, DB_NAME, json.optString(DB_NAME, null));
        addParameter(params, DB_SCHEMA, json.optString(DB_SCHEMA, null));
        addParameter(params, DB_USER, json.optString(DB_USER, null));
        addParameter(params, DB_PASSWORD, json.optString(DB_PASSWORD, null));
        addParameter(params, AUTHOR_NAME, json.optString(AUTHOR_NAME, null));
        addParameter(params, AUTHOR_EMAIL, json.optString(AUTHOR_EMAIL, null));
    }

    /**
     * Adds URL encoded request parameters to the parameter Map. Look for known parameters in the Form
     * and populate the supplied map with requested values. This method does the same thing as the
     * {@link #addParameters(java.util.Map, org.json.JSONObject)} version, except this one is for
     * Requests with a URL encoded form, as opposed to a JSON payload.
     *
     * @param params Map to hold the request parameters.
     * @param form   URL encoded Form from a Request with parameters encoded.
     */
    private void addParameters(Map<String, String> params, Form form) {
        addParameter(params, DIR_PARENT_DIR, form.getFirstValue(DIR_PARENT_DIR, null));
        addParameter(params, DB_HOST, form.getFirstValue(DB_HOST, null));
        addParameter(params, DB_PORT, form.getFirstValue(DB_PORT, null));
        addParameter(params, DB_NAME, form.getFirstValue(DB_NAME, null));
        addParameter(params, DB_SCHEMA, form.getFirstValue(DB_SCHEMA, null));
        addParameter(params, DB_USER, form.getFirstValue(DB_USER, null));
        addParameter(params, DB_PASSWORD, form.getFirstValue(DB_PASSWORD, null));
        addParameter(params, AUTHOR_NAME, form.getFirstValue(AUTHOR_NAME, null));
        addParameter(params, AUTHOR_EMAIL, form.getFirstValue(AUTHOR_EMAIL, null));
    }

    private Map<String, String> getRequestParameters(Request request) {
        HashMap<String, String> params = new HashMap<>(10);
        if (request.isEntityAvailable()) {
            Representation entity = request.getEntity();
            final MediaType reqMediaType = entity.getMediaType();

            if (MediaType.APPLICATION_WWW_FORM.equals(reqMediaType)) {
                // URL encoded form parameters
                try {
                    Form form = request.getEntityAsForm();
                    addParameters(params, form);
                } catch (Exception ex) {
                    throw new RestletException("Error parsing URL encoded form request",
                            CLIENT_ERROR_BAD_REQUEST, ex);
                }
            } else if (MediaType.APPLICATION_JSON.equals(reqMediaType)) {
                // JSON encoded parameters
                try {
                    JsonRepresentation jsonRep = new JsonRepresentation(entity);
                    JSONObject jsonObj = jsonRep.toJsonObject();
                    addParameters(params, jsonObj);
                } catch (IOException | JSONException ex) {
                    throw new RestletException("Error parsing JSON request",CLIENT_ERROR_BAD_REQUEST,
                            ex);
                }
            } else if (null != reqMediaType) {
                // unsupported MediaType
                throw new RestletException("Unsupported Request MediaType: " + reqMediaType,
                        CLIENT_ERROR_BAD_REQUEST);
            }
            // no parameters specified
        }
        // the request body was just consumed and can't be retrieved again. If we parsed Author info,
        // store that on the request for later processing.
        updateRequestWithAuthor(request, params);
        return params;
    }

    private void updateHintsWithParams(Hints hints, Map<String, String> params) {
        // get parameters
        final String parentDir = params.get(DIR_PARENT_DIR);
        final String dbHost = params.get(DB_HOST);
        final String dbPort = params.get(DB_PORT);
        final String dbName = params.get(DB_NAME);
        final String dbSchema = params.get(DB_SCHEMA);
        final String dbUser = params.get(DB_USER);
        final String dbPassword = params.get(DB_PASSWORD);
        // use parent directory if present
        if (parentDir != null) {
            final String leafDir = UUID.randomUUID().toString();
            final String uri = new File(parentDir, leafDir).getAbsoluteFile().toURI().toString();
            hints.set(Hints.REPOSITORY_URL, uri);
        } else if (dbName != null && dbPassword != null) {
            // try to build a URI from the db parameters
            PostgresConfigBean bean = new PostgresConfigBean();
            bean.setDatabase(dbName);
            bean.setPassword(dbPassword);
            // these have defaults in PostgresConfigBean, only overwrite defaults if present
            if (null != dbSchema) {
                bean.setSchema(dbSchema);
            }
            if (null != dbHost) {
                bean.setHost(dbHost);
            }
            if (null != dbUser) {
                bean.setUsername(dbUser);
            }
            if (null != dbPort) {
                try {
                    Integer portInt = Integer.parseInt(dbPort);
                    bean.setPort(portInt);
                } catch (Exception ex) {
                    // use the defaukt in PostgresConfigBean
                }
            }
            final String uri = bean.buildUriForRepo(
                    hints.get(Hints.REPOSITORY_NAME).get().toString()).toString();
            hints.set(Hints.REPOSITORY_URL, uri);
        }
    }

    @VisibleForTesting
    Hints createHintsFromRequest(Request request) {
        // get the repository name from the request
        final Optional<String> nameOptional = Optional.fromNullable(getStringAttribute(request,
                REPO_ATTR));
        if (!nameOptional.isPresent()) {
            // no repo name provided
            throw new RestletException(String.format(
                    "Cannot create GeoGIG repository. Missing '%s' resource", REPO_ATTR),
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
        final String repoName = nameOptional.get();
        final Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, repoName);
        // try to build the Repo URI from any Request parameters.
        updateHintsWithParams(hints, getRequestParameters(request));
        return hints;
    }

    static Optional<Repository> createGeoGIG(Request request) {
        final Hints hints = INSTANCE.createHintsFromRequest(request);
        // now build the repo with the Hints
        return Optional.fromNullable(RepositoryManager.get().createRepo(hints));
    }
}
