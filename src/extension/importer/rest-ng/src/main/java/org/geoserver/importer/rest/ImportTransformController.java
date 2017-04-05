/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import org.apache.commons.lang.NotImplementedException;
import org.geoserver.rest.catalog.CatalogController;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class ImportTransformController extends ImportBaseController {

    @Autowired
    protected ImportTransformController(Importer importer) {
        super(importer);
    }

    @PostMapping(path = { "/imports/{importId}/tasks/{taskId}/transforms" })
    public ResponseEntity postTransform(@PathVariable("importId") String importIdStr,
            @PathVariable("taskId") String taskIdStr,
            @RequestParam(value = "expand", required = false) String expand,
            @RequestBody ImportTransform importTransform, HttpServletRequest request,
            UriComponentsBuilder builder) {

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

    @GetMapping(path = {"/imports/{importId}/tasks/{taskId}/transforms",
            "/imports/{importId}/tasks/{taskId}/transforms/{transformId}" },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE })
    public ImportWrapper getTransform(@PathVariable("importId") Long importId, @PathVariable("taskId") Integer taskId,
                               @PathVariable(value = "transformId", required = false) Integer transformId,
                               @RequestParam(value = "expand", required = false) String expand, HttpServletRequest request) {

        return (writer, converter) -> {
            ImportTransform tx = transform(importId, taskId, transformId, true);
            if (tx == null) {
                converter.transformChain(task(importId, taskId), true, converter.expand(1));
            } else {
                ImportTask task = task(importId, taskId);
                int index = task.getTransform().getTransforms().indexOf(tx);

                converter.transform(tx, index, task, true, converter.expand(1));
            }
        };
    }

    @PutMapping(path = { "/imports/{importId}/tasks/{taskId}/transforms/{transformId}" },
            consumes = { MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON},
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE })
    public ImportWrapper putTransform(@PathVariable("importId") Long importId,
            @PathVariable("taskId") Integer taskId, @PathVariable("transformId") Integer transformId,
            @RequestParam(value = "expand", required = false) String expand,
            @RequestBody ImportTransform importTransform, HttpServletRequest request) {

        ImportTransform orig = transform(importId, taskId, transformId);
        OwsUtils.copy(importTransform, orig, (Class) orig.getClass());

        return (writer, converter) -> {
            ImportTask task = task(importId, taskId);
            int index = task.getTransform().getTransforms().indexOf(orig);

            converter.transform(orig, index, task, true, converter.expand(1));
        };
    }

    @DeleteMapping(path = { "/imports/{importId}/tasks/{taskId}/transforms/{transformId}" })
    public ResponseEntity deleteTransform(@PathVariable("importId") Long importId,
            @PathVariable("taskId") Integer taskId,
            @PathVariable("transformId") Integer transformId,
            @RequestParam(value = "expand", required = false) String expand,
            HttpServletRequest request) {

        ImportTask task = task(importId, taskId);
        ImportTransform tx = transform(importId, taskId, transformId);
        boolean result = task.getTransform().remove(tx);

        if (result) {
            return new ResponseEntity<String>("", HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
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
