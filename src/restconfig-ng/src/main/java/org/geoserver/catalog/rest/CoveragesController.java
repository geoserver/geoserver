/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.rest.RestException;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ControllerAdvice
@RequestMapping(path = "/restng/workspaces/{workspace}/coverages")
public class CoveragesController extends CatalogController {

    @Autowired
    public CoveragesController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE,
            TEXT_JSON})
    public RestWrapper<CoverageInfo> getWorkspaceCoverages(@PathVariable(name = "workspace") String workspaceName) {
        // get the workspace name space
        NamespaceInfo nameSpace = catalog.getNamespaceByPrefix(workspaceName);
        if (nameSpace == null) {
            // could not find the namespace associated with the desired workspace
            throw new RestException(String.format(
                    "Name space not found for workspace '%s'.", workspaceName), HttpStatus.NOT_FOUND);
        }
        // get all the coverages of the workspace \ name space
        List<CoverageInfo> coverages = catalog.getCoveragesByNamespace(nameSpace);
        return wrapList(coverages, CoverageInfo.class);
    }

    @GetMapping(path = "coverages/{coverage}", produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE,
            TEXT_JSON})
    public RestWrapper<CoverageInfo> getCoverage(@PathVariable(name = "workspace") String workspaceName,
                                                 @PathVariable(name = "coverage") String coverageName) {
        // get the workspace name space
        NamespaceInfo nameSpace = catalog.getNamespaceByPrefix(workspaceName);
        if (nameSpace == null) {
            // could not find the namespace associated with the desired workspace
            throw new RestException(String.format(
                    "Name space not found for workspace '%s'.", workspaceName), HttpStatus.NOT_FOUND);
        }
        CoverageInfo coverage = catalog.getCoverageByName(nameSpace, coverageName);
        return wrapObject(coverage, CoverageInfo.class);
    }
}
