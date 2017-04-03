/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.NotImplementedException;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import freemarker.template.ObjectWrapper;
import freemarker.template.Template;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class ImportTransformController extends ImportBaseController {

    @Autowired
    protected ImportTransformController(Importer importer) {
        super(importer);
    }

    @GetMapping(path = { "/imports/{importId}/tasks/{taskId}/transforms" }, produces = {
            MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public Object getTransforms(@PathVariable("importId") String importIdStr,
            @PathVariable("taskId") String taskIdStr,
            @RequestParam(value = "expand", required = false) String expand,
            HttpServletRequest request) {

        request.setAttribute("import", importIdStr);
        request.setAttribute("task", taskIdStr);
        request.setAttribute("expand", expand);

        Long taskId = taskIdStr == null ? null : Long.valueOf(taskIdStr);
        Integer importId = importIdStr == null ? null : Integer.valueOf(importIdStr);

        Object result = transform(true, importIdStr, taskIdStr, null);
        if (result == null) {
            result = task(taskId, importId).getTransform();
        }

        return result;
    }

    @PostMapping(path = { "/imports/{importId}/tasks/{taskId}/transforms" }, consumes = {})
    public ResponseEntity postTransform(@PathVariable("importId") String importIdStr,
            @PathVariable("taskId") String taskIdStr,
            @RequestParam(value = "expand", required = false) String expand,
            @RequestBody ImportTransform importTransform, HttpServletRequest request,
            UriComponentsBuilder builder) {

        request.setAttribute("import", importIdStr);
        request.setAttribute("task", taskIdStr);
        request.setAttribute("expand", expand);

        Long taskId = taskIdStr == null ? null : Long.valueOf(taskIdStr);
        Integer importId = importIdStr == null ? null : Integer.valueOf(importIdStr);

        ImportTransform tx = importTransform;
        ImportTask task = task(taskId, importId);
        task.getTransform().add(tx);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                builder.path("/imports/{importId}/tasks/{taskId}/transforms/{transformId}")
                        .buildAndExpand(importIdStr, taskIdStr,
                                task.getTransform().getTransforms().size() - 1)
                        .toUri());
        return new ResponseEntity<String>("", headers, HttpStatus.CREATED);
    }

    @GetMapping(path = {
            "/imports/{importId}/tasks/{taskId}/transforms/{transformId}" }, produces = {
                    MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public Object getTransform(@PathVariable("importId") String importIdStr,
            @PathVariable("taskId") String taskIdStr,
            @RequestParam(value = "expand", required = false) String expand,
            @PathVariable("transformId") String transformId, HttpServletRequest request) {

        request.setAttribute("import", importIdStr);
        request.setAttribute("task", taskIdStr);
        request.setAttribute("expand", expand);

        Long taskId = taskIdStr == null ? null : Long.valueOf(taskIdStr);
        Integer importId = importIdStr == null ? null : Integer.valueOf(importIdStr);

        Object result = transform(true, importIdStr, taskIdStr, transformId);
        if (result == null) {
            result = task(taskId, importId).getTransform();
        }

        return result;
    }

    @PutMapping(path = {
            "/imports/{importId}/tasks/{taskId}/transforms/{transformId}" }, consumes = {
                    MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.TEXT_HTML_VALUE,
                            MediaType.APPLICATION_JSON_VALUE })
    public ImportTransform putTransform(@PathVariable("importId") String importId,
            @PathVariable("taskId") String taskId, @PathVariable("transformId") String transformId,
            @RequestParam(value = "expand", required = false) String expand,
            @RequestBody ImportTransform importTransform, HttpServletRequest request) {

        request.setAttribute("import", importId);
        request.setAttribute("task", taskId);
        request.setAttribute("expand", expand);

        ImportTransform orig = transform(false, importId, taskId, transformId);
        OwsUtils.copy(importTransform, orig, (Class) orig.getClass());
        return orig;
    }

    @DeleteMapping(path = { "/imports/{importId}/tasks/{taskId}/transforms/{transformId}" })
    public ResponseEntity deleteTransform(@PathVariable("importId") String importIdStr,
            @PathVariable("taskId") String taskIdStr,
            @PathVariable("transformId") String transformIdStr,
            @RequestParam(value = "expand", required = false) String expand,
            HttpServletRequest request) {

        request.setAttribute("import", importIdStr);
        request.setAttribute("task", taskIdStr);
        request.setAttribute("expand", expand);

        Long taskId = taskIdStr == null ? null : Long.valueOf(taskIdStr);
        Integer importId = importIdStr == null ? null : Integer.valueOf(importIdStr);

        ImportTask task = task(taskId, importId);
        ImportTransform tx = transform(false, importIdStr, taskIdStr, transformIdStr);
        boolean result = task.getTransform().remove(tx);

        if (result) {
            return new ResponseEntity<String>("", HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
    }

    ImportTransform transform(boolean optional, String importId, String taskId,
            String transformId) {

        ImportTask task = task(taskId == null ? null : Long.valueOf(taskId),
                importId == null ? null : Integer.valueOf(importId));

        ImportTransform tx = null;
        if (transformId != null) {
            try {
                Integer i = Integer.parseInt(transformId);
                tx = (ImportTransform) task.getTransform().getTransforms().get(i);
            } catch (NumberFormatException e) {
            } catch (IndexOutOfBoundsException e) {
            }
        }

        if (tx == null && !optional) {
            throw new RestException("No such transform", HttpStatus.NOT_FOUND);
        }
        return tx;
    }

    /*
     * protected ImportTask task(String taskId, String importId) { return task(false, taskId, importId); }
     * 
     * protected ImportTask task(boolean optional, String taskId, String importId) { ImportContext context = context(importId); ImportTask task =
     * null;
     * 
     * String t = taskId; if (t != null) { int id = Integer.parseInt(t); task = context.task(id); } if (t != null && task == null) { throw new
     * RestException("No such task: " + t + " for import: " + context.getId(), HttpStatus.NOT_FOUND); }
     * 
     * if (task == null && !optional) { throw new RestException("No task specified", HttpStatus.NOT_FOUND); }
     * 
     * return task; }
     */

    protected ImportContext context(String importId) {
        return context(false, importId);
    }

    protected ImportContext context(boolean optional, String importId) {
        long i = Long.parseLong(importId);

        ImportContext context = importer.getContext(i);
        if (!optional && context == null) {
            throw new RestException("No such import: " + i, HttpStatus.NOT_FOUND);
        }
        return context;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return ImportTransform.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected String getTemplateName(Object object) {
        throw new NotImplementedException();
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        throw new NotImplementedException();
    }

    @Override
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {
        throw new NotImplementedException();
    }

}
