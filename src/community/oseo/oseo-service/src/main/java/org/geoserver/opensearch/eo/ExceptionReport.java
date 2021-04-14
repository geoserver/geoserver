/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Support class for encoding JSON exceptions according to
 * https://docs.opengeospatial.org/is/17-047r1/17-047r1.html#39
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExceptionReport {

    public static class Exception {
        String exceptionCode;
        String exceptionText;
        String locator;

        public Exception(String exceptionCode, String exceptionText, String locator) {
            this.exceptionCode = exceptionCode;
            this.exceptionText = exceptionText;
            this.locator = locator;
        }

        public String getExceptionCode() {
            return exceptionCode;
        }

        public void setExceptionCode(String exceptionCode) {
            this.exceptionCode = exceptionCode;
        }

        public String getExceptionText() {
            return exceptionText;
        }

        public void setExceptionText(String exceptionText) {
            this.exceptionText = exceptionText;
        }

        public String getLocator() {
            return locator;
        }

        public void setLocator(String locator) {
            this.locator = locator;
        }
    }

    String type = "Exception";
    List<Exception> exceptions = new ArrayList<>();

    public ExceptionReport(Exception exception) {
        this.type = type;
        this.exceptions.add(exception);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<Exception> exceptions) {
        this.exceptions = exceptions;
    }
}
