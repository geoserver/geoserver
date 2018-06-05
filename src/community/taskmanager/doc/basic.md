# GeoServer Task Manager - Basic Concepts

The two main kinds of objects of the [Task Manager](../readme.md) are [configurations](#configurations) and [batches](#batches). Task Manager also allows the creation of [Templates](#templates) for configurations.

## Configurations

The configuration is the central object in the Task Manager. A configuration is typically linked to a data object, such as a GeoServer layer, and serves as an entry point to the tasks and batches related to this data object.

A configuration has a unique name, a description and a workspace. It contains three groups of objects:

* `Attributes`: The attributes contain information about this configuration that can be shared between the different tasks of this configuration. An attribute has a name and a value. Each attribute is associated with at least one task parameter (see below). Attributes inherit their validation properties from their associated parameters, such as its accepted values and whether it is required.

* `Tasks`: Each task configures an operation that can be executed on this configuration. Each task has a name that is unique within the configuration, a type and a list of parameters with each a name and a value. The full name of a task is donated as *configuration-name/task-name* (which serves as a unique identifier for the task). The task's type is chosen from a [list of available task types](user.md#task-types) which define different kinds of operations (for example: copy a database table, publish a layer, ..) and expects a list of parameters that each has a name and a type. A parameter may or may not be required. The parameter type defines the accepted values of the parameter. Parameter types are dependent types when the list of accepted values depends on the value of another parameter (for example: tables inside a database). A parameter value is either a literal or a reference to an attribute of the form `${attribute-name}`.

* `Batches`.

## Batches

A batch is made of an ordered sequence of tasks that can either be run on demand or be scheduled to run repeatedly at specific times. There are two kinds of batches:

* `Configuration batches`: these are batches that belong to a configuration. All of the tasks inside this batch are tasks that belong to that same configuration.
* `Independent batches`: these are batches that do not belong to a configuration. They may contains tasks from any existing configuration.

A batch has a name, a description and a workspace. The name of a batch must be unique amongst its configuration or amongst all independent batches. The full name of a batch is denoted as *\[configuration-name:\]batch-name* which serves as a unique identifier for the batch. 

Configuration batches that have a name starting with a `@`, are hidden from the general batch overview and are only accessible from their configuration. Hidden batch names may be reserved for special functions. At this point, there is only one such case (see [Initializing templates](#templates)).

A batch can be run manually if the following conditions are met:

* the list of tasks is non-empty;
* the operating user has the security rights to do so (see [Security](user.md#security)).

A batch will be run automatically on its scheduled time if the following conditions are met:

* the list of tasks is non-empty;
* the batch is enabled;
* the batch has a frequency configured other than `NEVER`;
* the batch is independent or its configuration has been completed, i.e. validated without errors (in some cases a configuration may be saved before it is validated, see [Initializing templates](#templates)).

### Running a batch

The batch is executed in two phases:

* `RUN` phase: tasks are executed in the defined order. If an error occurs or the run is manually intermitted, cease execution and go to `ROLLBACK` phase. If all tasks finish succesfully, go to `COMMIT` phase.
* `COMMIT/ROLLBACK` phase: tasks are committed or rollbacked in the *opposite* order.

Consider a batch with three tasks 

*B = T1 -> T2 -> T3*.

A normal run would then be 

*run T1 -> run T2 -> run T3 -> commit T3 -> commit T2 -> commit T1*. 

However, if T2 fails, the run would be 

*run T1 -> run T2 (failure) -> rollback T1*. 

Most tasks support `COMMIT/ROLLBACK` by creating temporary objects that only become definite objects after a `COMMIT`. The `ROLLBACK` phase then simply cleans up those temporary objects. However, some particular [task types](user.md#task-types) may not support the `COMMIT/ROLLBACK` mechanism (in which case running them is definite).

The commit phase happens in opposite order because dependencies in the old version of the data often requires this. A concrete example may clear things up. Imagine that *T1* copies a database table *R* from one database to another, while *T2* creates a view *V* based on that table, so *V* depends on *R*. If the table and view already exist in older versions (*R_old* and *V_old*), they must not be removed until the `COMMIT` phase, so that their original state remains in the case of a `ROLLBACK`. During the `COMMIT` phase, *R_old* and *V_old* are removed, but it is not possible to remove *R_old* until *V_old* is removed. Therefore it is necessary to commit *T2* before *T1*.

The `COMMIT` phase typically replaces old objects with the new objects that have a temporary name. Since tasks often create objects that depend on objects of the previous tasks, these objects contain references to temporary names. Which means that when the temporary object is committed and becomes the real object, references in depending objects must also be updated. For this purpose, a tasks that uses a temporary object from a previous task registers a *dependency*, which is essentially an update added to the commit phase of that previous task. 

If *T3* has a dependency on task *T1* that we call *D1*, the following happens:

*run T1 -> run T2 -> run T3, register D1 -> commit T3 -> commit T2 -> commit T1, update D1*. 

Let's make it clearer again using an example. During the `RUN` phase *T1* creates table *R1_temp* and *T2* creates *V1_temp* that depends on *R1_temp*, this dependency will be registered. During the commit phase, *T2* will replace *V1* by *V1_temp*. Then, *T1* will replace *T1* by *T1_temp*. However, *V1* may still reference *T1_temp* which no longer exists. Therefore, *T1* will use the registered dependency to update *V1* to refer to *T1* instead of *T1_temp*.

Within a batch run, each task that has yet started has a status. These are the possible statuses:

* `RUNNING`: the task is currently running.
* `WAITING_TO_COMMIT`: the task has finished running, but is waiting to commit (or rollback) while other tasks are running or committing (or rolling back).
* `COMMITTING`: the task is currently committing.
* `ROLLING_BACK`: the task is currently rolling back.
* `COMMITTED`: the task was succesfully committed.
* `ROLLED_BACK`: the task was succesfully rolled back.
* `NOT_COMMITTED`: the task was supposed to commit but failed during the commit phase.
* `NOT_ROLLED_BACK`: the task was supposed to roll back but failed during roll back phase.

A task is consired finished if its status is not `RUNNING`, `WAITING_TO_COMMIT`, `ROLLING_BACK` or `COMMITTING`. A batch run does not have its own status, but it takes on the status of the last task that has started but is not `COMMITTED` or `ROLLED_BACK`. A batch run is considered finished if its status is not `RUNNING`, `WAITING_TO_COMMIT` or `COMMITTING`.

There is concurrency protection both on the level of tasks and batches. A single batch can never run simultaneously in multiple runs (the second run will wait for the first one to finish). A single task can never run simultaneously in multiple runs, even if part of a different batch. A single task can also not commit simultaneously in multiple runs. 

## Templates

Templates are in every way identical to configurations, with the exception of:

* they are never validated when saved (their attributes need not be filled in) and
* their tasks and batches can never be executed.

A template is used as a blueprint for the creation of configurations that are very similar to each other. Typically, the tasks are all the same but the attribute values are different. However, a template may also have attribute values filled in that serve as defaults. 

Once a configuration is created from a template, it is independent from that template (changes to the template do not affect it). The configuration can then be modified like any other configuration, including the removal, addition and manipulation of tasks.

### Initializing templates

An initializing template is any template that has a batch named `@Initialize` (case sensitive), which configures special behaviour. The purpose of this batch is to execute some tasks that must have been done at least once until some other tasks can actually be configured. For example, you may want to create a vector layer based on that table copied from a source database, then synchronise this layer to a target geoserver. The task that synchronizes a layer to the external geoserver will expect an existing configured layer, which you cannot create until you have copied the table first. The `@Initialize` batch would in this case copy the table from the source and create a layer in the local geoserver.

When creating a configuration from this template, configuration happens in two phases

* (1) Initially, only attributes related to tasks in the `@Initialize` batch must be configured. When the configuration is saved, the `@Initialize` batch is automatically executed.
* (2) Now, all other attributes and tasks must be configured and the configuration must be saved again.

This is the only case that a configuration can be saved before all the required attributes are filled in. Mind that batches will not be scheduled or visible in the general overview until the batch has been saved again (and the attributes have thus been validated).



