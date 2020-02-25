/**
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 *
 * @author David Vick, Boundless 2017
 */
package org.geoserver.script.rest.service;

import static java.lang.String.format;

import com.google.common.collect.Lists;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.rest.model.Script;
import org.geoserver.util.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ScriptService {

    @Autowired ScriptManager scriptMgr;

    String path;

    protected Resource script;

    public List<Script> getScriptList(HttpServletRequest request) {
        final String type = (String) request.getAttribute("type");
        this.path = getPath(request);
        this.path = stripExtension(this.path);

        Resource dir = scriptMgr.script(path);

        List<Script> scripts = Lists.newArrayList();
        if (dir != null) {
            Filter<Resource> filter =
                    type != null
                            ? new Filter<Resource>() {
                                @Override
                                public boolean accept(Resource pathname) {
                                    return type.equalsIgnoreCase(
                                            FilenameUtils.getExtension(pathname.name()));
                                }
                            }
                            : new Filter<Resource>() {
                                @Override
                                public boolean accept(Resource pathname) {
                                    return true;
                                }
                            };
            for (Resource f : Resources.list(dir, filter)) {
                if (path.equals("apps")) {
                    Resource mainScript = scriptMgr.findAppMainScript(f);
                    if (mainScript != null) {
                        String name =
                                mainScript
                                        .path()
                                        .substring(f.parent().path().length() + 1)
                                        .replace("\\", "/");
                        scripts.add(new Script(name));
                    }
                } else if (path.equals("wps")) {
                    if (f.getType() == Type.DIRECTORY) {
                        String namespace = f.name();
                        List<Resource> files = f.list();
                        for (Resource file : files) {
                            String name = namespace + ":" + file.name();
                            scripts.add(new Script(name));
                        }
                    } else {
                        String name = f.name();
                        scripts.add(new Script(name));
                    }
                } else {
                    String name = f.name();
                    scripts.add(new Script(name));
                }
            }
        }
        return scripts;
    }

    public void getScript(HttpServletRequest request, HttpServletResponse response) {
        this.path = getPath(request);

        try {
            if (path.contains(":")) {
                path = path.replace(":", "/");
            }
            script = scriptMgr.script(path);
        } catch (IllegalStateException e) {
            throw new RestException(
                    format("Error looking up script %s", path),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
        if (!Resources.exists(script)) {
            throw new RestException(format("Could not find script %s", path), HttpStatus.NOT_FOUND);
        }

        String fileContents;

        try {
            fileContents = FileUtils.readFileToString(script.file());
            response.getWriter().write(fileContents);
            response.setStatus(200);
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) {
        this.path = getPath(request);

        try {
            if (path.contains(":")) {
                path = path.replace(":", "/");
            }
            script = scriptMgr.script(path);
        } catch (IllegalStateException e) {
            throw new RestException(
                    format("Error creating script file %s", path),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }

        // copy over the contents
        try {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(script.out()))) {
                IOUtils.copy(request.getInputStream(), w);
                w.flush();
            }
        } catch (IOException e) {
            throw new RestException(
                    format("Error writing script file %s", path),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }

        response.setStatus(201);
    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) {
        this.path = getPath(request);

        try {
            if (path.contains(":")) {
                path = path.replace(":", "/");
            }
            script = scriptMgr.script(path);
            if (!Resources.exists(script)) {
                throw new IOException(format("Unable to find script file %s", path));
            }
        } catch (IOException e) {
            throw new RestException(
                    format("Error finding script file %s", path),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }

        boolean success = false;
        if (script != null && Resources.exists(script)) {
            success = script.delete();
            if (path.startsWith("apps")) {
                success = script.parent().delete();
            }
        }

        if (!success) {
            throw new RestException(
                    format("Error deleting script file %s", path),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setStatus(200);
    }

    private String getPath(HttpServletRequest request) {
        this.path = request.getPathInfo();
        path = ResponseUtils.stripBeginningPath(path);

        return path;
    }

    private String stripExtension(String path) {
        int i = path.lastIndexOf(".");
        if (i > -1) {
            return path.substring(0, i);
        } else {
            return path;
        }
    }
}
