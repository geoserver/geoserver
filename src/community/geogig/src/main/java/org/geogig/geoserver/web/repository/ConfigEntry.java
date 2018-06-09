/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static com.google.common.base.Objects.equal;

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

/** A config entry representation for the presentation layer */
public class ConfigEntry implements Serializable {
    private static final long serialVersionUID = -8750588422623774302L;

    private Integer id;

    private String name;

    private String value;

    private static String[] RESTRICTED_KEYS = {
        "repo.name",
        "storage.graph",
        "storage.refs",
        "storage.objects",
        "storage.index",
        "postgres.version",
        "rocksdb.version",
        "file.version"
    };

    public ConfigEntry() {
        this.name = "";
        this.value = "";
        this.id = null;
    }

    public ConfigEntry(String name, String value) {
        this.name = name;
        this.value = value;
        this.id = hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigEntry)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        ConfigEntry c = (ConfigEntry) o;
        return equal(name, c.name) && equal(value, c.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ConfigEntry.class, name, value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Nullable
    Integer getId() {
        return id;
    }

    public static boolean isRestricted(String key) {
        for (String restricted : RESTRICTED_KEYS) {
            if (restricted.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<ConfigEntry> fromConfig(Map<String, String> config) {
        ArrayList<ConfigEntry> configEntries = new ArrayList<>();
        for (Entry<String, String> entry : config.entrySet()) {
            configEntries.add(new ConfigEntry(entry.getKey(), entry.getValue()));
        }
        return configEntries;
    }
}
