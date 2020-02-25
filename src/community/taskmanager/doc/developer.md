# GeoServer Task Manager - Developer's Guide

[Task Manager](../readme.md)

* [Task Types](#task-types) - write your own operations
* [Actions](#actions) - extend the GUI for your tasks
* [Reporting](#reporting) - choose the content and destination of your batch reports

## Task Types

Task manager can be extended with custom made task types. Make your own implementation of the interface `TaskType` and let it be a spring bean.
The name provided via the `Named` interface is used as a reference.

```java

/**
 * A Task Type.
 * 
 */
public interface TaskType extends Named {
    
    /**
     * Return parameter info for this task type.
     * It is recommended to use a LinkedHashMap and add the parameters in a intuitive order.
     * This order will be preserved to present parameters to the user. 
     *
     * @return the parameter info
     */
    Map<String, ParameterInfo> getParameterInfo();
    
    /**
     * Run a task, based on these parameter values.
     * @param ctx task context
     * @return the task result
     */
    TaskResult run(TaskContext ctx) throws TaskException;
    
    /**
     * Do a clean-up for this task (for example, if this task publishes something, remove it).
     * @param ctx task context
     * @throws TaskException 
     */
    void cleanup(TaskContext ctx) throws TaskException;
    
    /**
     * task type can specify whether it supports clean-up or not
     * 
     * @return true if clean-up is supported
     */
    default boolean supportsCleanup() {
        return true;
    }

}
```

A ParameterInfo object contains a name, a [type](#parameter-type), whether they are required, and which other parameters they depend on
(for example, a database table depends on a database).

The Task context looks as follows:
```java
/**
 * Task Context used during batch run or task clean-up.
 * 
 */
public interface TaskContext {

    /**
     * @return the task
     */
    Task getTask();

    /**     
     * @return the batch context, null if this is a clean-up
     */
    BatchContext getBatchContext();

    /**
     * 
     * @return the parameter values, lazy loaded from task and configuration.
     * 
     * @throws TaskException
     */
    Map<String, Object> getParameterValues() throws TaskException;

    /**
     * Tasks can call this function to check if the user wants to interrupt the batch
     * and interrupt themselves.
     * If they do, they should still return a TaskResult that implements a roll back
     * of what was already done.
     * 
     * @return whether the batch run should be interrupted, false if this is a clean-up
     */
    boolean isInterruptMe();

}
```

The batch context looks as follows:
```java
/**
 * During run, tasks create temporary objects that are committed to real objects during
 * the commit phase (such as a table name) This maps real objects
 * to temporary objects during a single batch run. Tasks should save and look up temporary 
 * objects so that tasks within a batch can work together.
 * 
 */
public interface BatchContext {
    
    public static interface Dependency {
        public void revert() throws TaskException;
    }    

    Object get(Object original);

    Object get(Object original, Dependency dependency);

    /**
     * Whatever is put here in the task, must be removed in the commit!
     * 
     * @param original
     * @param temp
     */
    void put(Object original, Object temp);

    void delete(Object original) throws TaskException;

    BatchRun getBatchRun();

}
```

The task result looks as follows:
```java
/**
 * A handle of a task that was run but must still be committed or rolled back.
 * 
 *
 */
public interface TaskResult {

    /**
     * finalize and clean-up resources any roll-back data
     */
    void commit() throws TaskException;

    /**
     * batch has failed - cancel all changes
     */
    void rollback() throws TaskException;
    
}

```

This is an example of how a task type can create temporary object:

```java
 //inside TaskType.run method
 
 ctx.getBatchContext().put(originalObject, tempObject)
 
 ...
 
 return new TaskResult() {
 	@Override
 	public void commit() throws TaskException {
 		//this MUST be done!!!
 		ctx.getBatchContext.delete(originalObject)
 	} 
 
        ...
 
 }
 
```

Another task type would use this temporary object as follows:
```java
  //inside TaskType.run method
  
 Object tempObject = ctx.getBatchContext().get(originalObject, new Dependency() {
 	@Override
 	public void revert() {
 		Object object = ctx.getBatchContext().get(originalObject);
 		
 		mySomething.setMyProperty(object);
 		mySomething.save();
 	}
 });
 
 mySomething.setMyProperty(tempObject);
 mySomething.save();
```

### Parameter Types

Custom task types may use existing or define new parameter types. 
They handle parameter validation, parsing parameter Strings into other object types, and provide information to the GUI about the parameters.

Existing regular Parameter Types (static members of `ParameterType` interface):

* `STRING`
* `INTEGER`
* `BOOLEAN`
* `URI` 
* `SQL` (protects against ';' hacking)

External Parameter Types (members of `ExtTypes` spring bean):
* `dbName`: [database name](user.md#databases)
* `tableName`: table name (parameter must depend on parameter of `dbName` type)
* `extGeoserver`: [external geoserver](user.md#external-geoservers)
* `internalLayer`: layer from geoserver catalog
* `name`: name qualified with namespace from geoserver catalog
* `fileService`: [file service](user.md#file-services)
* `file`: reference to file (parameter must dpend of parameter of `fileService` type)

Defining a new Parameter Type:

```java
/**
 * 
 * A Parameter Type For a Task
 * 
 */
public interface ParameterType {
           
    /**
     * List possible values for this parameter (when applicable).
     * Include an empty string if custom value is also allowed.
     * 
     * @param dependsOnRawValues raw values of depending parameters.
     * @return list of possible values, null if not applicable.
     */
    public List<String> getDomain(List<String> dependsOnRawValues);
    
    /**
     * Validate and parse a parameter value for this parameter (at run time).
     * 
     * @param value the raw value.
     * @param dependsOnRawValues raw values of depending parameters.
     * @return the parsed value, NULL if the value is invalid.
     */
    public Object parse(String value, List<String> dependsOnRawValues);
    
    /**
     * Validate a parameter value (at configuration time).
     * 
     * @param value the raw value.
     * @param dependsOnRawValues raw values of depending parameters.
     * @return true if the value is considered valid at configuration time (may still be considered
     * invalid at parse time)
     */
    public default boolean validate(String value, List<String> dependsOnRawValues) {
        return parse(value, dependsOnRawValues) != null;
    }
    
    /**
     * Returns a list of web actions related to this type
     * 
     * @return list of web actions
     */
    public default List<String> getActions() {
        return Collections.emptyList();
    }

}
```

## Actions

Actions are extensions to the taskmanager webGUI attached to particular parameter types.

```java
public interface Action extends Named, Serializable {
    
    /**
     * Execute this action.
     * 
     * @param onPage the configuration page.
     * @param target the target of the ajax request that executed this action.
     * @param valueModel the value of the attribute, for reading and writing.
     * @param dependsOnRawValues raw values of depending attributes. 
     */
    void execute(ConfigurationPage onPage, AjaxRequestTarget target, IModel<String> valueModel, List<String> dependsOnRawValues);

    /**
     * Check whether this action can be executed with current values.
     * \
     * @param value the value of the attribute.
     * @param dependsOnRawValues raw values of depending attributes. 
     * @return whether this action accepts these values.
     */
    default boolean accept(String value, List<String> dependsOnRawValues) {
        return true;
    }

}
```

In order to be linked to parameter types, an action must be spring bean. The name provided via the `Named` interface is used as a reference.

## Reporting

### Report builders 

Reports are user friendly representations of finished batch runs, that are sent to some destination right after the batch run
has finished. A report has a type (`FAILED`, `CANCELLED` or `SUCCESS`), a title and a content. Use spring to configure 
a single report builder.

```java
/**
 * A report builder generates a report from a batch.
 * One could write a custom one.
 *
 */
public interface ReportBuilder {
    
    Report buildBatchRunReport(BatchRun batchRun);

}
```

### Report services.

Use spring to configure any number of report services.

```java
/**
 * A report service sends a report to a particular destination.
 * One can add an unlimited amount of report services which will all be used.
 *
 */
public interface ReportService {
    
    /**
     * Enumeration for filter.
     *
     */
    public enum Filter {
        /** All batch runs are reported **/
        ALL (Report.Type.FAILED, Report.Type.CANCELLED, Report.Type.SUCCESS),
        /** Only failed and cancelled batch runs are reported **/
        FAILED_AND_CANCELLED (Report.Type.FAILED, Report.Type.CANCELLED), 
        /** Only failed batch runs are reported **/
        FAILED_ONLY (Report.Type.FAILED);
        
        Report.Type[] types;
        
        private Filter(Report.Type... types) {
            this.types = types;
        }
        
        public boolean matches(Report.Type type) {
            return ArrayUtils.contains(types, type);
        }
        
    }
    
    /**
     * Return the filter of the report.
     * 
     * @return the filter of the report.
     */
    public Filter getFilter();
    
    /**
     * Send a report.
     * 
     * @param report the report.
     */
    public void sendReport(Report report);

}
```


