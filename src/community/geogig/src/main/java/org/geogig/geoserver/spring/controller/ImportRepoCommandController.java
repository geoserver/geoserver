/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.controller;

import static org.locationtech.geogig.rest.repository.RepositoryProvider.BASE_REPOSITORY_ROUTE;
import static org.locationtech.geogig.rest.repository.RepositoryProvider.GEOGIG_ROUTE_PREFIX;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.bind.annotation.RequestMethod.TRACE;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geogig.geoserver.spring.dto.RepositoryImportRepo;
import org.geogig.geoserver.spring.service.ImportRepoService;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.spring.controller.AbstractController;
import org.locationtech.geogig.spring.dto.InitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for importing an existing repository. */
@RestController
@RequestMapping(
    path = GEOGIG_ROUTE_PREFIX + "/" + BASE_REPOSITORY_ROUTE + "/{repoName}/importExistingRepo",
    produces = {APPLICATION_XML_VALUE, APPLICATION_JSON_VALUE}
)
public class ImportRepoCommandController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportRepoCommandController.class);

    @Autowired private ImportRepoService importRepoService;

    @RequestMapping(method = {GET, PUT, DELETE, PATCH, TRACE, OPTIONS})
    public void catchAll() {
        // if we hit this controller, it's a 405
        supportedMethods(Sets.newHashSet(POST.toString()));
    }

    @PostMapping
    public void importRepositoryNoBody(
            @PathVariable(name = "repoName") String repoName,
            HttpServletRequest request,
            HttpServletResponse response)
            throws RepositoryConnectionException {
        RepositoryImportRepo repo = importRepo(request, repoName);
        encode(repo, request, response);
    }

    @PostMapping(consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public void importRepositoryFromJsonOrXml(
            @PathVariable(name = "repoName") String repoName,
            @RequestBody InitRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response)
            throws RepositoryConnectionException {

        RepositoryImportRepo repo = importRepo(request, repoName, requestBody);
        encode(repo, request, response);
    }

    @PostMapping(consumes = {APPLICATION_FORM_URLENCODED_VALUE})
    public void importRepositoryFromForm(
            @PathVariable(name = "repoName") String repoName,
            @RequestBody MultiValueMap<String, String> requestBody,
            HttpServletRequest request,
            HttpServletResponse response)
            throws RepositoryConnectionException {
        RepositoryImportRepo repo = importRepo(request, repoName, requestBody);
        encode(repo, request, response);
    }

    private RepositoryImportRepo importRepo(HttpServletRequest request, String repoName)
            throws RepositoryConnectionException {
        Optional<RepositoryProvider> repoProvider = getRepoProvider(request);
        if (repoProvider.isPresent()) {
            return importRepoService.importRepository(
                    repoProvider.get(), repoName, Maps.newHashMap());
        } else {
            throw NO_PROVIDER;
        }
    }

    private RepositoryImportRepo importRepo(
            HttpServletRequest request, String repoName, InitRequest requestBody)
            throws RepositoryConnectionException {
        Optional<RepositoryProvider> repoProvider = getRepoProvider(request);
        if (repoProvider.isPresent()) {
            return importRepoService.importRepository(
                    repoProvider.get(),
                    repoName,
                    (requestBody == null) ? Maps.newHashMap() : requestBody.getParameters());
        } else {
            throw NO_PROVIDER;
        }
    }

    private RepositoryImportRepo importRepo(
            HttpServletRequest request, String repoName, MultiValueMap<String, String> requestBody)
            throws RepositoryConnectionException {
        Optional<RepositoryProvider> repoProvider = getRepoProvider(request);
        if (repoProvider.isPresent()) {
            return importRepoService.importRepository(
                    repoProvider.get(),
                    repoName,
                    (requestBody == null) ? Maps.newHashMap() : requestBody.toSingleValueMap());
        } else {
            throw NO_PROVIDER;
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
