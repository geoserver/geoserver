/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import org.geoserver.monitor.Monitor;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/monitor/requests/ows/{request}")
public class OwsRequestResource extends MonitorRequestController {

    @Autowired
    public OwsRequestResource(Monitor monitor) {
        super(monitor);
    }

    @GetMapping(produces = {CSV_MEDIATYPE_VALUE, EXCEL_MEDIATYPE_VALUE, ZIP_MEDIATYPE_VALUE})
    @ResponseBody
    protected RestWrapper handleObjectGetRestWrapper(
            @PathVariable(name = "request", required = false) String req,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "offset", required = false) Long offset,
            @RequestParam(name = "count", required = false) Long count,
            @RequestParam(name = "live", required = false) Boolean live,
            @RequestParam(name = "fields", required = false) String fieldsSpec)
            throws Exception {
        return super.handleObjectGetRestWrapper(
                req, from, to, filter, order, offset, count, live, fieldsSpec);
    }

    @GetMapping(
        produces = {
            MediaType.TEXT_HTML_VALUE,
            CSV_MEDIATYPE_VALUE,
            EXCEL_MEDIATYPE_VALUE,
            ZIP_MEDIATYPE_VALUE
        }
    )
    @ResponseBody
    protected MonitorQueryResults handleObjectGet(
            @PathVariable(name = "request", required = false) String req,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "offset", required = false) Long offset,
            @RequestParam(name = "count", required = false) Long count,
            @RequestParam(name = "live", required = false) Boolean live,
            @RequestParam(name = "fields", required = false) String fieldsSpec)
            throws Exception {
        if (req == null) {
            String[] fields = getFields(fieldsSpec);
            return new MonitorQueryResults(monitor.getDAO().getOwsRequests(), fields, monitor);
        } else {
            return handleObjectGet(req, from, to, filter, order, offset, count, live, fieldsSpec);
        }
    }
}
