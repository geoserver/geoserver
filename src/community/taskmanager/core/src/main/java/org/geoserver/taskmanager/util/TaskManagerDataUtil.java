/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Run.Status;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation independent helper methods.
 *
 * @author Niels Charlier
 */
@Service
public class TaskManagerDataUtil {

    private static Pattern PATTERN_ATTRIBUTEREF = Pattern.compile("^\\$\\{(.*)\\}$");

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDao dao;

    @Autowired private BatchJobService bjService;

    // -------------------------
    // Non-transactional methods
    // -------------------------

    /**
     * Set parameter of a task.
     *
     * @param task the task.
     * @param name the parameter name.
     * @param attName the attribute name.
     */
    public void setTaskParameter(final Task task, final String name, final String value) {
        Parameter pam = task.getParameters().get(name);
        if (pam == null) {
            pam = fac.createParameter();
            pam.setTask(task);
            pam.setName(name);
            task.getParameters().put(name, pam);
        }
        pam.setValue(value);
    }

    /**
     * Set parameter of a task associated with an attribute.
     *
     * @param task the task.
     * @param name the parameter name.
     * @param attName the attribute name.
     */
    public void setTaskParameterToAttribute(
            final Task task, final String name, final String attName) {
        setTaskParameter(task, name, "${" + attName + "}");
    }

    /**
     * Set attribute of a configuration.
     *
     * @param config the configuration.
     * @param name the attribute name.
     * @param value the attribute value.
     */
    public void setConfigurationAttribute(
            final Configuration config, final String name, final String value) {
        Attribute att = config.getAttributes().get(name);
        if (att == null) {
            att = fac.createAttribute();
            att.setConfiguration(config);
            att.setName(name);
            config.getAttributes().put(name, att);
        }
        att.setValue(value);
    }

    /**
     * Add a task to configuration.
     *
     * @param config the configuration.
     * @param task the task.
     */
    public void addTaskToConfiguration(final Configuration config, final Task task) {
        if (config.getTasks().get(task.getName()) != null) {
            throw new IllegalArgumentException("task name already exists in configuration");
        }
        task.setConfiguration(config);
        config.getTasks().put(task.getName(), task);
    }

    /**
     * Add a batch to configuration.
     *
     * @param config the configuration.
     * @param batch the batch.
     */
    public void addBatchToConfiguration(final Configuration config, final Batch batch) {
        if (config.getBatches().get(batch.getName()) != null) {
            throw new IllegalArgumentException("batch name already exists in configuration");
        }
        batch.setConfiguration(config);
        config.getBatches().put(batch.getName(), batch);
    }

    /**
     * Add a batch element to a batch at the end of the batch. If a batch element with this
     * combination batch/task already exists, (even if it has been soft removed) this batch element
     * will be activated if necessary and returned.
     *
     * @param batch the batch.
     * @param task the task.
     * @return the new or existing batch element.
     */
    public BatchElement addBatchElement(final Batch batch, final Task task) {
        BatchElement batchElement = getOrCreateBatchElement(batch, task);
        if (!batch.getElements().contains(batchElement)) {
            batch.getElements().add(batchElement);
            batchElement.setBatch(batch);
        }
        if (!task.getBatchElements().contains(batchElement)) {
            task.getBatchElements().add(batchElement);
            batchElement.setTask(task);
        }
        return batchElement;
    }

    /**
     * Add a batch element to a batch on a particular position. If a batch element with this
     * combination batch/task already exists, (even if it has been soft removed) this batch element
     * will be activated if necessary and returned.
     *
     * @param batch the batch.
     * @param task the task.
     * @param position the position.
     * @return the new or existing batch element.
     */
    public BatchElement addBatchElement(final Batch batch, final Task task, final int position) {
        BatchElement batchElement = getOrCreateBatchElement(batch, task);
        batch.getElements().remove(batchElement);
        batch.getElements().add(position, batchElement);
        batchElement.setBatch(batch);
        if (!task.getBatchElements().contains(batchElement)) {
            task.getBatchElements().add(batchElement);
            batchElement.setTask(task);
        }
        return batchElement;
    }

    private BatchElement getOrCreateBatchElement(final Batch batch, final Task task) {
        BatchElement batchElement = null;
        if (batch.getId() != null) {
            batchElement = dao.getBatchElement(batch, task);
            if (batchElement != null) {
                batchElement.setActive(true);
            }
        }
        if (batchElement == null) {
            batchElement = fac.createBatchElement();
            batchElement.setTask(task);
            batchElement.setBatch(batch);
        }
        return batchElement;
    }

    public String getAssociatedAttributeName(Parameter param) {
        if (param.getValue() != null) {
            Matcher matcher = PATTERN_ATTRIBUTEREF.matcher(param.getValue());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public Set<String> getAssociatedAttributeNames(Task task) {
        Set<String> attNames = new HashSet<String>();
        for (Parameter pam : task.getParameters().values()) {
            String attName = getAssociatedAttributeName(pam);
            if (attName != null) {
                attNames.add(attName);
            }
        }
        return attNames;
    }

    /**
     * List all associated parameters of attribute
     *
     * @param att the attribute
     * @return the parameters
     */
    public List<Parameter> getAssociatedParameters(Attribute att, Configuration config) {
        List<Parameter> result = new ArrayList<Parameter>();
        for (Task task : config.getTasks().values()) {
            for (Parameter param : task.getParameters().values()) {
                if (att.getName().equals(getAssociatedAttributeName(param))) {
                    result.add(param);
                }
            }
        }
        return result;
    }

    /**
     * Verifiy if batch is deletable (not running)
     *
     * @param batch the batch to verify
     * @return whether it is deletable
     */
    public boolean isDeletable(Batch batch) {
        return dao.getCurrentBatchRuns(batch).isEmpty();
    }

    /**
     * Verifiy if configuration is deletable (no batches are running)
     *
     * @param config the config to verify
     * @return whether it is deletable
     */
    @Transactional("tmTransactionManager")
    public boolean isDeletable(Configuration config) {
        config = dao.reload(config);
        for (Batch batch : config.getBatches().values()) {
            if (!dao.getCurrentBatchRuns(batch).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // -----------------------
    // Transactional methods
    // -----------------------

    @Transactional("tmTransactionManager")
    public Configuration saveScheduleAndRemove(
            Configuration config, Collection<Task> tasks, Collection<Batch> batches) {
        config = bjService.saveAndSchedule(config);
        for (Task task : tasks) {
            dao.remove(task);
        }
        for (Batch batch : batches) {
            bjService.remove(batch);
        }
        return config;
    }

    @Transactional("tmTransactionManager")
    public Batch saveScheduleAndRemove(Batch batch, Collection<BatchElement> bes) {
        batch = bjService.saveAndSchedule(batch);
        for (BatchElement be : bes) {
            dao.remove(be);
        }
        return batch;
    }

    /**
     * Run a batch element if possible (i.e. if the task is not being run already).
     *
     * @param element the batch element.
     * @return the run, or null if the task is being run elsewhere)
     */
    @Transactional("tmTransactionManager")
    public Run runIfPossible(BatchElement element, BatchRun br) {
        if (dao.getCurrentRun(element.getTask()) == null) {
            Run run = fac.createRun();
            br.getRuns().add(run);
            run.setBatchRun(br);
            run.setStart(new Date());
            run.setBatchElement(element);
            return dao.save(run);
        } else {
            return null;
        }
    }

    /**
     * Commit a batch element if possible (i.e. if the task is not being committed already).
     *
     * @param element the batch element.
     * @return the run, or null if the task is being committed elsewhere)
     */
    @Transactional("tmTransactionManager")
    public Run startCommitIfPossible(Run run) {
        if (dao.getCommittingRun(run.getBatchElement().getTask()) == null) {
            run.setStatus(Run.Status.COMMITTING);
            return dao.save(run);
        } else {
            return null;
        }
    }

    /**
     * Close a batch run (do this when the batch run is no longer running, but its status suggests
     * it is.)
     */
    @Transactional("tmTransactionManager")
    public BatchRun closeBatchRun(BatchRun br, String message) {
        for (Run run : br.getRuns()) {
            if (!run.getStatus().isClosed()) {
                if (run.getEnd() == null) {
                    // the task stopped while running
                    run.setStatus(Status.FAILED);
                    run.setEnd(new Date());
                } else if (br.getStatus() == Status.COMMITTING) {
                    // if the batch run was already in the commit phase,
                    // we were already past the point of rolling back
                    run.setStatus(Status.NOT_COMMITTED);
                } else { // the task was finished, but was neither committed or rolled back
                    run.setStatus(Status.NOT_ROLLED_BACK);
                }
                run.setMessage(message);
                br = dao.save(run).getBatchRun();
            }
        }
        return dao.reload(br);
    }

    @Transactional("tmTransactionManager")
    public BatchElement taskInUse(Task task, Configuration config) {
        if (task.getId() != null) {
            task = dao.reload(task);
            for (BatchElement element : task.getBatchElements()) {
                if (element.getBatch().isActive()) {
                    return element;
                }
            }
        } else {
            config = dao.reload(config);
            for (Batch batch : config.getBatches().values()) {
                for (BatchElement element : batch.getElements()) {
                    if (element.getTask().equals(task)) {
                        return element;
                    }
                }
            }
        }
        return null;
    }

    @Transactional("tmTransactionManager")
    public BatchElement taskInUseByExternalBatch(Configuration config) {
        config = dao.reload(config);
        for (Task task : config.getTasks().values()) {
            for (BatchElement element : task.getBatchElements()) {
                if (element.getBatch().getConfiguration() == null
                        && element.getBatch().isActive()) {
                    return element;
                }
            }
        }
        return null;
    }
}
