/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.geofence.core.dao.DuplicateKeyException;
import org.geoserver.geofence.server.rest.xml.AbstractPayload;
import org.geoserver.geofence.server.rest.xml.Batch;
import org.geoserver.geofence.server.rest.xml.BatchOperation;
import org.geoserver.geofence.server.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.server.rest.xml.JaxbRule;
import org.geoserver.geofence.services.exception.BadRequestServiceEx;
import org.geoserver.geofence.services.exception.InternalErrorServiceEx;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.geoserver.geofence.services.exception.WebApplicationException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.util.logging.Logging;
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

    private static final Logger LOGGER = Logging.getLogger(BatchRestController.class);

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
    public void notFound(
            NotFoundServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @ExceptionHandler(BadRequestServiceEx.class)
    public void badRequest(
            BadRequestServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(InternalErrorServiceEx.class)
    public void internalServerError(
            BadRequestServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(500, exception.getMessage());
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
        try {
            List<BatchOperation> operations = batch.getOperations();
            for (BatchOperation op : operations) {
                execBatchOp(op);
            }
            return HttpStatus.OK;
        } catch (DuplicateKeyException e) {
            throw new BadRequestServiceEx(
                    "The operation is trying to add a duplicate rule or adminrule");
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error: " + ex.getMessage(), ex);
            throw new InternalErrorServiceEx("Unexpected exception: " + ex.getMessage());
        }
    }

    private void execBatchOp(BatchOperation op) {
        BatchOperation.ServiceName sn = op.getService();
        ensureService(sn);
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
        ensureType(type);
        switch (type) {
            case insert:
                ensureRulePayload(op);
                rulesRestController.insert((JaxbRule) op.getPayload());
                break;
            case delete:
                ensureId(op, BatchOperation.TypeName.delete.name());
                rulesRestController.delete(op.getId());
                break;
            case update:
                ensureRulePayload(op);
                ensureId(op, BatchOperation.TypeName.update.name());
                rulesRestController.update(op.getId(), (JaxbRule) op.getPayload());
                break;
            default:
                throw new BadRequestServiceEx(
                        "No batch op found in context of rule service, for type " + type != null
                                ? type.name()
                                : "");
        }
    }

    private void executeAdminRuleOp(BatchOperation op) {
        BatchOperation.TypeName type = op.getType();
        ensureType(type);
        switch (type) {
            case insert:
                ensureAdminRulePayload(op);
                adminRulesRestController.insert((JaxbAdminRule) op.getPayload());
                break;
            case delete:
                ensureId(op, BatchOperation.TypeName.delete.name());
                adminRulesRestController.delete(op.getId());
                break;
            case update:
                ensureAdminRulePayload(op);
                ensureId(op, BatchOperation.TypeName.update.name());
                adminRulesRestController.update(op.getId(), (JaxbAdminRule) op.getPayload());
                break;
            default:
                throw new BadRequestServiceEx(
                        "No batch op found in context of adminrule service, for type " + type
                                        != null
                                ? type.name()
                                : "");
        }
    }

    private void ensureRulePayload(BatchOperation op) {
        AbstractPayload jaxbPayload = op.getPayload();
        if (!(jaxbPayload instanceof JaxbRule)) {
            throw new BadRequestServiceEx("An operation requiring a Rule payload doesn't have it");
        }
    }

    private void ensureAdminRulePayload(BatchOperation op) {
        AbstractPayload jaxbPayload = op.getPayload();
        if (!(jaxbPayload instanceof JaxbAdminRule)) {
            throw new BadRequestServiceEx(
                    "An operation requiring an AdminRule payload doesn't have it");
        }
    }

    private void ensureId(BatchOperation op, String type) {
        if (op.getId() == null)
            throw new BadRequestServiceEx("An id is required for operation type " + type);
    }

    private void ensureType(BatchOperation.TypeName typeName) {
        if (typeName == null)
            throw new BadRequestServiceEx(
                    "The operation type is mandatory but on or more operation elements doesn't have it");
    }

    private void ensureService(BatchOperation.ServiceName serviceName) {
        if (serviceName == null)
            throw new BadRequestServiceEx(
                    "The operation service is mandatory but on or more operation elements doesn't have it");
    }
}
