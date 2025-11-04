/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Configuration;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "configuration"})})
public class AttributeImpl extends BaseImpl implements Attribute {

    @Serial
    private static final long serialVersionUID = 7379737906910394714L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    @XStreamOmitField
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
