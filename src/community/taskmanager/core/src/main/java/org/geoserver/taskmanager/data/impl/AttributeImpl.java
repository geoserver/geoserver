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
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Configuration;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "configuration"})})
public class AttributeImpl extends BaseImpl implements Attribute {

    private static final long serialVersionUID = 7379737906910394714L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 8192)
    private byte[] value;

    @ManyToOne
    @JoinColumn(name = "configuration")
    private ConfigurationImpl configuration;

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
        return value == null ? null : new String(value);
    }

    @Override
    public void setValue(String value) {
        this.value = value == null ? null : value.getBytes();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public ConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = (ConfigurationImpl) configuration;
    }
}
