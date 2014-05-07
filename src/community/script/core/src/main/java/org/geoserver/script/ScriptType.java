package org.geoserver.script;

public enum ScriptType {

    APP("App"),
    FUNCTION("Function"),
    WPS("WPS"),
    WFSTX("WFS/TX");
    
    private final String label;
    
    ScriptType(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
    
    public static ScriptType getByLabel(String label) {
        for(ScriptType type : ScriptType.values()) {
            if (type.getLabel().equalsIgnoreCase(label) || type.name().equalsIgnoreCase(label)) {
                return type;
            }
        }
        return null;
    }
    
}
