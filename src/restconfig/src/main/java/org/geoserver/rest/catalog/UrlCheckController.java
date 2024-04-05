/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.urlchecks.AbstractURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping(
        path = RestBaseController.ROOT_PATH + "/urlchecks",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        })
public class UrlCheckController extends RestBaseController {

    private static final Logger LOGGER = Logging.getLogger(UrlCheckController.class);
    private final URLCheckDAO urlCheckDao;

    @Autowired
    public UrlCheckController(URLCheckDAO urlCheckDAO) {
        this.urlCheckDao = urlCheckDAO;
    }

    @GetMapping
    public RestWrapper<AbstractURLCheck> urlChecksGet() throws IOException {
        List<AbstractURLCheck> checks = urlCheckDao.getChecks();
        return wrapList(checks, AbstractURLCheck.class);
    }

    @GetMapping("/{urlCheckName}")
    public RestWrapper<AbstractURLCheck> urlCheckGet(@PathVariable String urlCheckName)
            throws IOException {

        AbstractURLCheck check = urlCheckDao.getCheckByName(urlCheckName);
        if (check == null) {
            throw new ResourceNotFoundException("No such URL check found: '" + urlCheckName + "'");
        }

        LOGGER.log(Level.INFO, "GET {0}", urlCheckName);
        LOGGER.log(Level.INFO, "got {0}", check.getName());

        return wrapObject(check, AbstractURLCheck.class);
    }

    @PostMapping(
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
            })
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> urlCheckPost(
            @RequestBody AbstractURLCheck urlCheck, UriComponentsBuilder builder) {
        try {

            if (urlCheckDao.getCheckByName(urlCheck.getName()) != null) {
                throw new RestException(
                        "URL check '" + urlCheck.getName() + "' already exists",
                        HttpStatus.CONFLICT);
            }

            verifyCheckName(urlCheck);
            verifyCheckConfiguration(urlCheck);

            /* enforce URL check enabled status by default */
            if (urlCheck.isEnabled() == null) {
                urlCheck.setEnabled(true);
            }
            urlCheckDao.save(urlCheck);

            String name = urlCheck.getName();
            LOGGER.log(Level.INFO, "Added urlCheck {0}", name);
            LOGGER.log(Level.INFO, "POST urlCheck {0}", name);

            return new ResponseEntity<>(
                    name, composeResponseHeaders(builder, name), HttpStatus.CREATED);

        } catch (IOException ex) {
            throw new RestException(
                    "Error occurred in creating a new URL check",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex);
        }
    }

    private static void verifyCheckName(AbstractURLCheck urlCheck) {
        String checkName = urlCheck.getName();
        if (checkName == null || checkName.isEmpty()) {
            throw new RestException("The URL check name is required", HttpStatus.BAD_REQUEST);
        }
    }

    private HttpHeaders composeResponseHeaders(UriComponentsBuilder builder, String name) {
        HttpHeaders headers = new HttpHeaders();
        UriComponents uriComponents = composeUriComponents(name, builder);
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return headers;
    }

    private UriComponents composeUriComponents(String name, UriComponentsBuilder builder) {
        return builder.path("/urlchecks/{id}").buildAndExpand(name);
    }

    @PutMapping(
            value = "/{urlCheckName}",
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
            })
    @ResponseStatus(HttpStatus.OK)
    public void workspacePut(
            @RequestBody AbstractURLCheck providedCheck, @PathVariable String urlCheckName) {
        try {

            AbstractURLCheck urlCheck = urlCheckDao.getCheckByName(urlCheckName);
            if (urlCheck == null) {
                throw new RestException(
                        "Can't change a non existent URL check (" + urlCheckName + ")",
                        HttpStatus.NOT_FOUND);
            }

            if (providedCheck.getConfiguration() != null) {
                verifyCheckConfiguration(providedCheck);
                urlCheck.setConfiguration(providedCheck.getConfiguration());
            }
            if (providedCheck.getDescription() != null) {
                urlCheck.setDescription(providedCheck.getDescription());
            }
            if (providedCheck.isEnabled() != null) {
                urlCheck.setEnabled(providedCheck.isEnabled());
            }

            urlCheckDao.save(urlCheck);

        } catch (IOException ex) {
            throw new RestException(
                    "Error occurred in changing the URL check",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex);
        }
    }

    private static void verifyCheckConfiguration(AbstractURLCheck urlCheck) {
        String checkConfiguration = urlCheck.getConfiguration();
        if (checkConfiguration == null || checkConfiguration.isEmpty()) {
            throw new RestException(
                    "The URL check configuration is required", HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/{urlCheckName}")
    protected void urlCheckDelete(@PathVariable String urlCheckName) {
        try {

            AbstractURLCheck check = urlCheckDao.getCheckByName(urlCheckName);
            if (check == null) {
                throw new ResourceNotFoundException(
                        "No such URL check found: '" + urlCheckName + "'");
            }

            urlCheckDao.removeByName(urlCheckName);

            LOGGER.log(Level.INFO, "DELETE urlCheck {0}", urlCheckName);

        } catch (IOException ex) {
            throw new RestException(
                    "Error occurred in deleting the URL check",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex);
        }
    }
}
