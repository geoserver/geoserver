/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.collector;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

@Component
public class OSHISystemInfoCollector extends BaseSystemInfoCollector {

    private SystemInfo si;

    private OperatingSystem os;

    private HardwareAbstractionLayer hal;

    private CentralProcessor pr;

    public OSHISystemInfoCollector() {
        si = new SystemInfo();
        os = si.getOperatingSystem();
        hal = si.getHardware();
        pr = hal.getProcessor();
    }

    @Override
    SystemInfoProperty retriveSystemInfo(SystemInfoProperty systemInfo) {
        SystemInfoProperty si = super.retriveSystemInfo(systemInfo);
        switch (systemInfo) {
        case OS_TYPE:
            si.addValue(new SystemPropertyValue("", "", "",
                    os.getFamily() + " " + os.getVersion().getVersion()));
            si.setAvailable(true);
            break;
        case CPU_USAGE:
            double[] loads = pr.getProcessorCpuLoadBetweenTicks();
            List<String> values = Arrays.stream(loads).boxed().map(v -> String.format("%.2f", v))
                    .collect(Collectors.toList());
            for (int i = 0; i < values.size(); i++) {
                si.addValue(new SystemPropertyValue("cpu", "" + (i + 1), "usage", values.get(i)));
            }
            si.setAvailable(true);
            break;
        default:
            break;
        }
        return si;
    }

}
