/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "task"})})
public class ParameterImpl extends BaseImpl implements Parameter {

    private static final long serialVersionUID = 2728548577251702332L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column private String value;

    @ManyToOne
    @JoinColumn(name = "task")
    private TaskImpl task;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Task getTask() {
        return task;
    }

    @Override
    public void setTask(Task task) {
        this.task = (TaskImpl) task;
    }
}
