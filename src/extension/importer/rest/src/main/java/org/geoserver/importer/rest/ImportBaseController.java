/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.springframework.http.HttpStatus;

public class ImportBaseController extends RestBaseController {
    protected Importer importer;

    protected ImportBaseController(Importer importer) {
        this.importer = importer;
    }

    protected ImportContext context(Long imp) {
        return context(imp, false);
    }

    protected ImportContext context(Long imp, boolean optional) {
        return (ImportContext) context(imp, optional, false);
    }

    Object context(Long imp, boolean optional, boolean allowAll) {
        if (imp == null) {
            if (allowAll) {
                return importer.getAllContexts();
            }
            if (optional) {
                return null;
            }
            throw new RestException("No import specified", HttpStatus.BAD_REQUEST);
        } else {
            ImportContext context = null;
            context = importer.getContext(imp);
            if (context == null && !optional) {
                throw new RestException("No such import: " + imp.toString(), HttpStatus.NOT_FOUND);
            }
            return context;
        }
    }

    protected ImportTask task(Long imp, Integer taskNumber) {
        return task(imp, taskNumber, false);
    }

    protected ImportTask task(Long imp, Integer taskNumber, boolean optional) {
        return (ImportTask) task(imp, taskNumber, optional, false);
    }

    protected Object task(Long imp, Integer taskNumber, boolean optional, boolean allowAll) {
        ImportContext context = context(imp);
        ImportTask task = null;

        // handle null taskNumber
        if (taskNumber == null) {
            if (!optional && !allowAll) {
                throw new RestException("No task specified", HttpStatus.NOT_FOUND);
            }
        } else {
            task = context.task(taskNumber);
        }

        // handle no task found
        if (task == null) {
            if (allowAll) {
                return context.getTasks();
            }
            if (!optional) {
                throw new RestException(
                        "No such task: " + taskNumber + " for import: " + context.getId(),
                        HttpStatus.NOT_FOUND);
            }
        }
        return task;
    }

    ImportTransform transform(Long importId, Integer taskId, Integer transformId) {
        return transform(importId, taskId, transformId, false);
    }

    ImportTransform transform(
            Long importId, Integer taskId, Integer transformId, boolean optional) {
        ImportTask task = task(importId, taskId);

        return transform(task, transformId, optional);
    }

    ImportTransform transform(ImportTask task, Integer transformId, boolean optional) {
        ImportTransform tx = null;
        if (transformId != null) {
            try {
                tx = (ImportTransform) task.getTransform().getTransforms().get(transformId);
            } catch (NumberFormatException e) {
            } catch (IndexOutOfBoundsException e) {
            }
        }

        if (tx == null && !optional) {
            throw new RestException("No such transform", HttpStatus.NOT_FOUND);
        }
        return tx;
    }

    protected int expand(int def, String ex) {
        if (ex == null) {
            return def;
        }
        try {
            return "self".equalsIgnoreCase(ex)
                    ? 1
                    : "all".equalsIgnoreCase(ex)
                            ? Integer.MAX_VALUE
                            : "none".equalsIgnoreCase(ex) ? 0 : Integer.parseInt(ex);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
