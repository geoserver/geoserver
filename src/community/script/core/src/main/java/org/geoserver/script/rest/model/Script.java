package org.geoserver.script.rest.model;

public class Script {

    private String name;

    public Script(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
