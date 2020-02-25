/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/taskmanager-import")
public class ImportTool {

    private static final Logger LOGGER = Logging.getLogger(ImportTool.class);

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    private static final String SPLIT_BY = ";";

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{template}", method = RequestMethod.POST)
    public void doImportWithTemplate(
            @PathVariable String template,
            @RequestBody String csvFile,
            @RequestParam(defaultValue = "true") boolean validate)
            throws IOException {

        if (!SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .contains(GeoServerRole.ADMIN_ROLE)) {
            throw new AccessDeniedException("You must be administrator.");
        }

        try (Scanner scanner = new Scanner(csvFile)) {
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] attNames = line.split(SPLIT_BY);

                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    String[] split = line.split(SPLIT_BY);
                    Map<String, String> record = new HashMap<String, String>();
                    for (int i = 0; i < Math.min(attNames.length, split.length); i++) {
                        record.put(attNames[i], split[i]);
                    }

                    String configName = record.remove("name");
                    Configuration config = dao.getConfiguration(configName);
                    if (config == null) {
                        config = dao.copyConfiguration(template);
                        config.setName(configName);
                    } else {
                        config = dao.init(config);
                    }
                    config.setTemplate(false);
                    if (record.containsKey("description")) {
                        config.setDescription(record.remove("description"));
                    }
                    if (record.containsKey("workspace")) {
                        config.setWorkspace(record.remove("workspace"));
                    }

                    for (Map.Entry<String, String> entry : record.entrySet()) {
                        dataUtil.setConfigurationAttribute(
                                config, entry.getKey(), entry.getValue());
                    }

                    if (validate) {
                        List<ValidationError> errors = taskUtil.validate(config);
                        if (!errors.isEmpty()) {
                            for (ValidationError error : errors) {
                                LOGGER.severe(
                                        "Failed to import configuration "
                                                + config.getName()
                                                + ", validation error: "
                                                + error.toString());
                            }
                        } else {
                            config.setValidated(true);
                            try {
                                bjService.saveAndSchedule(config);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.SEVERE,
                                        "Failed to import configuration " + config.getName(),
                                        e);
                            }
                        }
                    } else {
                        try {
                            bjService.saveAndSchedule(config);
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failed to import configuration " + config.getName(),
                                    e);
                        }
                    }
                }
            }
        }
    }
}
