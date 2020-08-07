package org.geotools.dggs.rhealpix;

import java.util.List;
import java.util.stream.Collectors;
import jep.JepException;
import jep.SharedInterpreter;

class RHealPixUtils {

    public static void setCellId(SharedInterpreter interpreter, String variableName, String id)
            throws JepException {
        interpreter.set(variableName, toInternalId(id));
        interpreter.exec(variableName + "= tuple(" + variableName + ")");
    }

    private static List<Object> toInternalId(String id) {
        return id.chars()
                .mapToObj(c -> c >= '0' && c <= '9' ? c - '0' : String.valueOf((char) c))
                .collect(Collectors.toList());
    }
}
