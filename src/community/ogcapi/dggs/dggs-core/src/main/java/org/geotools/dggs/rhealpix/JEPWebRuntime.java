/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.rhealpix;

import java.util.logging.Logger;
import jep.JepException;
import jep.SharedInterpreter;
import org.geotools.util.logging.Logging;

/**
 * {@link JEPRuntime} handling shared interpreters as thread locals. Some per request mechanisms
 * should be used to clean up the interpreters at the end of the request, calling {@link
 * #closeThreadIntepreter()}
 */
public class JEPWebRuntime implements JEPRuntime {

    static final Logger LOGGER = Logging.getLogger(JEPWebRuntime.class);
    private final Initializer initializer;
    static final ThreadLocal<SharedInterpreter> INTERPRETER = new ThreadLocal<>();

    public JEPWebRuntime(Initializer initializer) {
        this.initializer = initializer;
    }

    public void dispose() {
        // no disposing needed, some per request handling is needed
    }

    /**
     * Closes the {@link SharedInterpreter} associated to the current thread
     *
     * @throws JepException
     */
    public static void closeThreadIntepreter() throws JepException {
        @SuppressWarnings("PMD.CloseResource")
        SharedInterpreter interpreter = INTERPRETER.get();
        INTERPRETER.remove();
        if (interpreter != null) {
            interpreter.close();
        }
    }

    @Override
    public SharedInterpreter getInterpreter() throws JepException {
        @SuppressWarnings("PMD.CloseResource")
        SharedInterpreter interpreter = INTERPRETER.get();
        if (interpreter == null) {
            interpreter = new SharedInterpreter();
            initializer.initalize(interpreter);
            INTERPRETER.set(interpreter);
        }
        return interpreter;
    }

    <T> T run(InterpreterFunction<T> function, ExceptionHandler handler) throws Exception {
        try {
            return function.accept(getInterpreter());
        } catch (JepException e) {
            Exception result = handler.accept(e);
            if (result != null) {
                throw result;
            } else {
                return null;
            }
        }
    }

    <T> T runSafe(InterpreterFunction<T> function, RuntimeExceptionHandler handler) {
        try {
            return function.accept(getInterpreter());
        } catch (JepException e) {
            RuntimeException result = handler.accept(e);
            if (result != null) {
                throw result;
            } else {
                return null;
            }
        }
    }

    <T> T runSafe(InterpreterFunction<T> function) {
        try {
            return function.accept(getInterpreter());
        } catch (JepException e) {
            throw new RuntimeException(e);
        }
    }
}
