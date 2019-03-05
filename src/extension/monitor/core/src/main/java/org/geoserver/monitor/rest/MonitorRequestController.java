/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.Converters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    path = {
        RestBaseController.ROOT_PATH + "/monitor/requests/{request}",
        RestBaseController.ROOT_PATH + "/monitor/requests"
    }
)
public class MonitorRequestController extends RestBaseController {

    static final String CSV_MEDIATYPE_VALUE = "application/csv";

    static final String ZIP_MEDIATYPE_VALUE = "application/zip";

    static final String EXCEL_MEDIATYPE_VALUE = "application/vnd.ms-excel";

    static final MediaType EXCEL_MEDIATYPE = MediaType.valueOf(EXCEL_MEDIATYPE_VALUE);

    static final MediaType ZIP_MEDIATYPE = MediaType.valueOf(ZIP_MEDIATYPE_VALUE);

    static final MediaType CSV_MEDIATYPE = MediaType.valueOf(CSV_MEDIATYPE_VALUE);

    Monitor monitor;

    @Autowired
    public MonitorRequestController(Monitor monitor) {
        this.monitor = monitor;
    }

    String[] getFields(String fields) {
        if (fields != null) {
            return fields.split(";");
        } else {
            List<String> props = OwsUtils.getClassProperties(RequestData.class).properties();

            props.remove("Class");
            props.remove("Body");
            props.remove("Error");

            return props.toArray(new String[props.size()]);
        }
    }

    @GetMapping(
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
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
        MonitorQueryResults results =
                handleObjectGet(req, from, to, filter, order, offset, count, live, fieldsSpec);
        Object object = results.getResult();

        // HTML specific bits
        if (object instanceof RequestData) {
            return wrapObject((RequestData) object, RequestData.class);
        } else {
            final List<RequestData> requests = new ArrayList<>();
            BaseMonitorConverter.handleRequests(
                    object,
                    new RequestDataVisitor() {
                        public void visit(RequestData data, Object... aggregates) {
                            requests.add(data);
                        }
                    },
                    monitor);
            return wrapList(requests, RequestData.class);
        }
    }

    /**
     * Template method to get a custom template name
     *
     * @param o The object being serialized.
     */
    protected String getTemplateName(Object o) {
        if (o instanceof RequestData) {
            return "request.html";
        } else {
            return "requests.html";
        }
    }

    @GetMapping(produces = {CSV_MEDIATYPE_VALUE, EXCEL_MEDIATYPE_VALUE, ZIP_MEDIATYPE_VALUE})
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
        String[] fields = getFields(fieldsSpec);

        if (req == null) {
            Query q =
                    new Query()
                            .between(
                                    from != null ? parseDate(from) : null,
                                    to != null ? parseDate(to) : null);

            // filter
            if (filter != null) {
                try {
                    parseFilter(filter, q);
                } catch (Exception e) {
                    throw new RestException(
                            "Error parsing filter " + filter, HttpStatus.BAD_REQUEST, e);
                }
            }

            // sorting
            String sortBy;
            SortOrder sortOrder;
            if (order != null) {
                int semi = order.indexOf(';');
                if (semi != -1) {
                    String[] split = order.split(";");
                    sortBy = split[0];
                    sortOrder = SortOrder.valueOf(split[1]);
                } else {
                    sortBy = order;
                    sortOrder = SortOrder.ASC;
                }

                q.sort(sortBy, sortOrder);
            }

            // limit offset
            q.page(offset, count);

            // live?
            if (live != null) {
                if (live) {
                    q.filter(
                            "status",
                            Arrays.asList(
                                    org.geoserver.monitor.RequestData.Status.RUNNING,
                                    org.geoserver.monitor.RequestData.Status.WAITING,
                                    org.geoserver.monitor.RequestData.Status.CANCELLING),
                            Comparison.IN);
                } else {
                    q.filter(
                            "status",
                            Arrays.asList(
                                    org.geoserver.monitor.RequestData.Status.FINISHED,
                                    org.geoserver.monitor.RequestData.Status.FAILED),
                            Comparison.IN);
                }
            }

            return new MonitorQueryResults(q, fields, monitor);
        } else {
            // return the individual
            RequestData data = monitor.getDAO().getRequest(Long.parseLong(req));
            if (data == null) {
                throw new ResourceNotFoundException("No such request" + req);
            }
            return new MonitorQueryResults(data, fields, monitor);
        }
    }

    Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
        } catch (ParseException e) {
            return Converters.convert(s, Date.class);
        }
    }

    void parseFilter(String filter, Query q) {
        for (String s : filter.split(";")) {
            if ("".equals(s.trim())) continue;
            String[] split = s.split(":");

            String left = split[0];
            Object right = split[2];
            if (right.toString().contains(",")) {
                List list = new ArrayList();
                for (String t : right.toString().split(",")) {
                    list.add(parseProperty(left, t));
                }
                right = list;
            } else {
                right = parseProperty(left, right.toString());
            }

            q.and(left, right, Comparison.valueOf(split[1]));
        }
    }

    Object parseProperty(String property, String value) {
        if ("status".equals(property)) {
            return org.geoserver.monitor.RequestData.Status.valueOf(value);
        }

        return value;
    }

    @DeleteMapping
    public void handleObjectDelete(@PathVariable(name = "request", required = false) String req) {
        if (req == null) {
            monitor.getDAO().clear();
        } else {
            throw new RestException(
                    "Cannot delete a specific request", HttpStatus.METHOD_NOT_ALLOWED);
        }
    }
}
