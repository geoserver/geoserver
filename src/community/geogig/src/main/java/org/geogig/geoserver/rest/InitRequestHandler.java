/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.getStringAttribute;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Status;

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

    private Optional<Form> getRequestForm(Request request) {
        final HashMap<String, String> map = new HashMap<>();
        if (request.isEntityAvailable()) {
            // see if there is a Form in the request
            try {
                Form requestForm = request.getEntityAsForm();
                return Optional.fromNullable(requestForm);
            } catch (Exception ex) {
                // no form, eat the exception
            }
        }
        return Optional.absent();
    }

    private void updateHintsWithForm(Hints hints, Form form) {
        // get paramsters
        final String parentDir = form.getFirstValue(DIR_PARENT_DIR, null);
        final String dbHost = form.getFirstValue(DB_HOST, null);
        final String dbPort = form.getFirstValue(DB_PORT, null);
        final String dbName = form.getFirstValue(DB_NAME, null);
        final String dbSchema = form.getFirstValue(DB_SCHEMA, null);
        final String dbUser = form.getFirstValue(DB_USER, null);
        final String dbPassword = form.getFirstValue(DB_PASSWORD, null);
        // use parent directory if present
        if (parentDir != null) {
            final String leafDir = UUID.randomUUID().toString();
            final String uri = new File(parentDir, leafDir).getAbsoluteFile().toURI().toString();
            hints.set(Hints.REPOSITORY_URL, uri);
        } else if (dbName != null && dbUser != null && dbPassword != null) {
            // try to build a URI from the db parameters
            PostgresConfigBean bean = new PostgresConfigBean();
            bean.setDatabase(dbName);
            bean.setUsername(dbUser);
            bean.setPassword(dbPassword);
            // these have defaults in PostgresConfigBean, only overwrite defaults if present
            if (null != dbSchema) {
                bean.setSchema(dbSchema);
            }
            if (null != dbHost) {
                bean.setHost(dbHost);
            }
            if (null != dbPort) {
                try {
                    Integer portInt = Integer.parseInt(dbPort);
                    bean.setPort(portInt);
                } catch (Exception ex) {
                    // use the defaukt in PostgresConfigBean
                }
            }
            final String uri = bean.buildUriForRepo(UUID.randomUUID().toString()).toString();
            hints.set(Hints.REPOSITORY_URL, uri);
        }
    }

    private Hints createHintsFromRequest(Request request) {
        // get the repository name from the request
        final Optional<String> nameOptional = Optional.of(getStringAttribute(request, REPO_ATTR));
        if (!nameOptional.isPresent()) {
            // no repo name provided
            throw new RestletException(String.format(
                    "Cannot create GeoGIG repository. Missing '%s' resource", REPO_ATTR),
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
        final String repoName = nameOptional.get();
        final Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, repoName);
        // try to build the Repo URI from any Form parameters in the request.
        Optional<Form> optional = getRequestForm(request);
        if (optional.isPresent()) {
            updateHintsWithForm(hints, optional.get());
        }
        return hints;
    }

    static Optional<GeoGIG> createGeoGIG(Request request) {
        final Hints hints = INSTANCE.createHintsFromRequest(request);
        // now build the repo with the Hints
        return Optional.fromNullable(RepositoryManager.get().createRepo(hints));
    }
}
