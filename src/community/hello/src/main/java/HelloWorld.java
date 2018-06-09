/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.vfny.geoserver.ExceptionHandler;
import org.vfny.geoserver.servlets.AbstractService;

public class HelloWorld extends AbstractService {

    public HelloWorld() {
        super("HWS", "SayHello", null);
    }

    protected ExceptionHandler getExceptionHandler() {
        return null;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.getOutputStream().write("Hello World".getBytes());
    }
}
