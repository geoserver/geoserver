/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import java.io.IOException;
import java.lang.reflect.Type;
import org.apache.commons.lang3.NotImplementedException;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
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

@RestController
@ControllerAdvice
@RequestMapping(
        path = RestBaseController.ROOT_PATH + "/imports/{importId}",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE})
public class ImportTransformController extends ImportBaseController {

    @Autowired
    protected ImportTransformController(Importer importer) {
        super(importer);
    }

    @PostMapping(path = {"/tasks/{taskId}/transforms"})
    public ResponseEntity postTransform(
            @PathVariable Long importId,
            @PathVariable Integer taskId,
            @RequestBody ImportTransform tx,
            UriComponentsBuilder builder)
            throws IOException {

        ImportTask task = task(importId, taskId);
        @SuppressWarnings("unchecked")
        TransformChain<ImportTransform> transforms = (TransformChain) task.getTransform();
        transforms.add(tx);
        importer.changed(task);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path("/imports/{importId}/tasks/{taskId}/transforms/{transformId}")
                .buildAndExpand(
                        importId.toString(),
                        taskId.toString(),
                        transforms.getTransforms().size() - 1)
                .toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>("", headers, HttpStatus.CREATED);
    }

    @GetMapping(path = {"/tasks/{taskId}/transforms", "/tasks/{taskId}/transforms/{transformId}"})
    public ImportWrapper getTransform(
            @PathVariable Long importId,
            @PathVariable Integer taskId,
            @PathVariable(required = false) Integer transformId,
            @RequestParam(value = "expand", required = false) String expand) {

        return (writer, builder, converter) -> {
            ImportTransform tx = transform(importId, taskId, transformId, true);
            if (tx == null) {
                converter.transformChain(builder, task(importId, taskId), true, converter.expand(expand, 1));
            } else {
                ImportTask task = task(importId, taskId);
                int index = task.getTransform().getTransforms().indexOf(tx);

                converter.transform(builder, tx, index, task, true, converter.expand(expand, 1));
            }
        };
    }

    @PutMapping(
            path = {"/tasks/{taskId}/transforms/{transformId}"},
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaTypeExtensions.TEXT_JSON_VALUE})
    public ImportWrapper putTransform(
            @PathVariable Long importId,
            @PathVariable Integer taskId,
            @PathVariable Integer transformId,
            @RequestParam(value = "expand", required = false) String expand,
            @RequestBody ImportTransform importTransform)
            throws IOException {

        ImportTask task = task(importId, taskId);
        ImportTransform orig = transform(task, transformId, false);
        @SuppressWarnings("unchecked")
        Class<ImportTransform> txc = (Class<ImportTransform>) orig.getClass();
        OwsUtils.copy(importTransform, orig, txc);
        importer.changed(task);

        return (writer, builder, converter) -> {
            int index = task.getTransform().getTransforms().indexOf(orig);

            converter.transform(builder, orig, index, task, true, converter.expand(expand, 1));
        };
    }

    @DeleteMapping(path = {"/tasks/{taskId}/transforms/{transformId}"})
    public ResponseEntity deleteTransform(
            @PathVariable Long importId, @PathVariable Integer taskId, @PathVariable Integer transformId)
            throws IOException {

        ImportTask task = task(importId, taskId);
        ImportTransform tx = transform(task, transformId, true);
        @SuppressWarnings("unchecked")
        TransformChain<ImportTransform> transforms = (TransformChain) task.getTransform();
        boolean result = transforms.remove(tx);

        if (result) {
            importer.changed(task);
            return new ResponseEntity<>("", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ImportTransform.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected String getTemplateName(Object object) {
        throw new NotImplementedException("ImportTransformController::getTemplateName() is not implemented");
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        throw new NotImplementedException("ImportTransformController::createObjectWrapper() is not implemented");
    }

    @Override
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {
        throw new NotImplementedException("ImportTransformController::configueFreemarker() is not implemented");
    }
}
