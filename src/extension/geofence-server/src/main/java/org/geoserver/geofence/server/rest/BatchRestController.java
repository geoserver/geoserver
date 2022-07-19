package org.geoserver.geofence.server.rest;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.geofence.rest.xml.AbstractPayload;
import org.geoserver.geofence.rest.xml.Batch;
import org.geoserver.geofence.rest.xml.BatchOperation;
import org.geoserver.geofence.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.services.exception.BadRequestServiceEx;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/geofence/batch")
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class BatchRestController extends RestBaseController {

    @Autowired private AdminRulesRestController adminRulesRestController;

    @Autowired private RulesRestController rulesRestController;

    /**
     * Exception handle to hanlde the {@link NotFoundServiceEx} exceptiont throw by the service.
     *
     * @param exception the exception to handle.
     * @param request the request.
     * @param response the response.
     * @throws IOException
     */
    @ExceptionHandler(NotFoundServiceEx.class)
    public void ruleNotFound(
            NotFoundServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(404, exception.getMessage());
    }

    /**
     * Execute a Batch with all the operations it includes.
     *
     * @param batch the batch to execute.
     * @return 200 when successful error otherwise.
     */
    @RequestMapping(
            value = "/exec",
            method = RequestMethod.POST,
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    @Transactional(value = "geofenceTransactionManager")
    public HttpStatus exec(@RequestBody(required = true) Batch batch) {
        List<BatchOperation> operations = batch.getOperations();
        for (BatchOperation op : operations) {
            execBatchOp(op);
        }
        return HttpStatus.OK;
    }

    private void execBatchOp(BatchOperation op) {
        BatchOperation.ServiceName sn = op.getService();
        if (sn == null)
            throw new NotFoundServiceEx("Service is mandatory but has not been provided.");
        switch (sn) {
            case rules:
                executeRuleOp(op);
                break;
            case adminrules:
                executeAdminRuleOp(op);
                break;
            default:
                throw new NotFoundServiceEx(
                        "No service found for name " + sn != null ? sn.name() : "");
        }
    }

    private void executeRuleOp(BatchOperation op) {
        BatchOperation.TypeName type = op.getType();
        if (type == null)
            throw new NotFoundServiceEx("Operation type is mandatory but has not been provided.");
        switch (type) {
            case insert:
                ensureRulePayload(op);
                rulesRestController.insert((JaxbRule) op.getPayload());
                break;
            case delete:
                rulesRestController.delete(op.getId());
                break;
            case update:
                ensureRulePayload(op);
                rulesRestController.update(op.getId(), (JaxbRule) op.getPayload());
                break;
            default:
                throw new BadRequestServiceEx(
                        "No batch op found for type " + type != null ? type.name() : "");
        }
    }

    private void executeAdminRuleOp(BatchOperation op) {
        BatchOperation.TypeName type = op.getType();
        if (type == null)
            throw new NotFoundServiceEx("Operation type is mandatory but has not been provided.");
        switch (type) {
            case insert:
                ensureAdminRulePayload(op);
                adminRulesRestController.insert((JaxbAdminRule) op.getPayload());
                break;
            case delete:
                adminRulesRestController.delete(op.getId());
                break;
            case update:
                ensureAdminRulePayload(op);
                adminRulesRestController.update(op.getId(), (JaxbAdminRule) op.getPayload());
                break;
            default:
                throw new BadRequestServiceEx(
                        "No batch op found for type " + type != null ? type.name() : "");
        }
    }

    private void ensureRulePayload(BatchOperation op) {
        AbstractPayload jaxbPayload = op.getPayload();
        if (!(jaxbPayload instanceof JaxbRule)) {
            throw new NotFoundServiceEx("No Rule payload has been sent into the request.");
        }
    }

    private void ensureAdminRulePayload(BatchOperation op) {
        AbstractPayload jaxbPayload = op.getPayload();
        if (!(jaxbPayload instanceof JaxbAdminRule)) {
            throw new NotFoundServiceEx("No Rule payload has been sent into the request.");
        }
    }
}
