package com.boundlessgeo.gsr.model.map;

/**
 * Basic information about a {@link LayerOrTable}, for use in service listings
 */
public class LayerEntry {
    public final Integer id;
    public final String name;

    public LayerEntry(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
