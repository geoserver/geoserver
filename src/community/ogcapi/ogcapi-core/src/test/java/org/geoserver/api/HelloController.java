/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */
package org.geoserver.api;

import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.platform.ServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(
    service = "Hello",
    version = "1.0",
    landingPage = "ogc/hello",
    serviceClass = HelloController.HelloServiceInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH)
public class HelloController {

    static interface HelloServiceInfo extends ServiceInfo {};

    String defaultValue = "hello";

    @GetMapping(path = "hello", name = "sayHello")
    @ResponseBody
    public Message hello(@RequestParam(name = "message", required = false) String message) {
        return new Message(message != null ? message : defaultValue);
    }

    @PostMapping(path = "echo")
    @ResponseBody
    public ResponseEntity echo(@RequestBody Message message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<String>(message.getMessage(), headers, HttpStatus.CREATED);
    }

    @DeleteMapping(path = "delete")
    @ResponseBody
    public ResponseEntity<String> delete() {
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(path = "default")
    public void putDefault(@RequestBody String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @GetMapping(path = "noContent")
    public void httpErrorCodeException() {
        throw new HttpErrorCodeException(HttpServletResponse.SC_NO_CONTENT);
    }

    @GetMapping(path = "wrappedException")
    public void wrappedHttpErrorCodeException() {
        try {
            throw new HttpErrorCodeException(HttpServletResponse.SC_NO_CONTENT);
        } catch (Exception e) {
            throw new ServiceException("Wrapping code error", e);
        }
    }

    @GetMapping(path = "badRequest")
    public void badRequestHttpErrorCodeException() {
        throw new HttpErrorCodeException(HttpServletResponse.SC_BAD_REQUEST);
    }

    @GetMapping(path = "errorWithPayload")
    public void httpErrorCodeExceptionWithContentType() {
        throw new HttpErrorCodeException(HttpServletResponse.SC_OK, "{\"hello\":\"world\"}")
                .setContentType("application/json");
    }
}
