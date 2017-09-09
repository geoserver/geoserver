/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.Sensors;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * 
 * Retrieve real system information metrics defined in {@link MetricInfo} using OSHI library
 * <p>
 * Is possible to extends this class to change or add others low level APIs and use this new class
 * as main collector after register using autowiring.
 * 
 * @see <a href="https://github.com/oshi/oshi">OSHI library </a>
 * @author sandr
 *
 */
@Component
public class OSHISystemInfoCollector extends BaseSystemInfoCollector {

    private static final long serialVersionUID = 502867203324474735L;

    private static Log log = LogFactory.getLog(OSHISystemInfoCollector.class);

    private SystemInfo si;

    private OperatingSystem os;

    private HardwareAbstractionLayer hal;

    private CentralProcessor pr;

    private GlobalMemory mm;

    private Sensors ss;

    private FileSystem fs;

    public OSHISystemInfoCollector() {
        si = new SystemInfo();
        os = si.getOperatingSystem();
        hal = si.getHardware();
        pr = hal.getProcessor();
        mm = hal.getMemory();
        ss = hal.getSensors();
        fs = os.getFileSystem();
    }

    @Override
    List<MetricValue> retriveSystemInfo(MetricInfo info) {
        List<MetricValue> si = super.retriveSystemInfo(info);
        try {
            switch (info) {
            case OS_TYPE: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(os.getFamily() + " " + os.getVersion().getVersion());
                si = Collections.singletonList(mv);
                break;
            }
            case UPTIME: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(String.format("%.2f", pr.getSystemUptime() / 3600d));
                si = Collections.singletonList(mv);
                break;
            }
            case SYSTEM_AVERAGE_LOAD: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(String.format("%.2f", pr.getSystemCpuLoad() * 100));
                si = Collections.singletonList(mv);
                break;
            }
            case SYSTEM_MEMORY_USAGE_P: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(String.format("%.2f", mm.getAvailable() / (double) (1L << 20)) + "/"
                        + String.format("%.2f", mm.getTotal() / (double) (1L << 20)));
                si = Collections.singletonList(mv);
                break;
            }
            case SYSTEM_MEMORY_USAGE_S: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(String.format("%.2f", mm.getSwapUsed() / (double) (1L << 20)) + "/"
                        + String.format("%.2f", mm.getSwapUsed() / (double) (1L << 20)));
                si = Collections.singletonList(mv);
                break;
            }
            case PHYSICAL_CPU_N: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(Integer.toString(pr.getPhysicalProcessorCount()));
                si = Collections.singletonList(mv);
                break;
            }
            case LOGICAL_CPU_N: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(Integer.toString(pr.getLogicalProcessorCount()));
                si = Collections.singletonList(mv);
                break;
            }
            case CPU_LOAD: {
                si = new ArrayList<MetricValue>();
                double[] loads = pr.getProcessorCpuLoadBetweenTicks();
                for (int i = 0; i < loads.length; i++) {
                    String value = String.format("%.2f", loads[i]);
                    String name = info.name() + "_" + (i + 1);
                    String description = "CPU " + (i + 1) + " Load";
                    MetricValue mv = new MetricValue(info);
                    mv.setValue(value);
                    mv.setAvailable(true);
                    mv.setDescription(description);
                    mv.setName(name);
                    si.add(mv);
                }
                break;
            }
            case RUNNING_PROCESS_N: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(Integer.toString(os.getProcessCount()));
                si = Collections.singletonList(mv);
                break;
            }
            case RUNNING_THREADS_N: {
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(Integer.toString(os.getThreadCount()));
                si = Collections.singletonList(mv);
                break;
            }
            case MOUNTED_FS_USAGE: {
                OSFileStore[] fss = fs.getFileStores();
                if (fss.length > 0) {
                    si = new ArrayList<MetricValue>();
                    for (int i = 0; i < fss.length; i++) {
                        OSFileStore fs = fss[i];
                        MetricValue mv = new MetricValue(info);
                        mv.setName(info.name() + "_" + (i + 1));
                        mv.setValue(String.format("%.2f", fs.getUsableSpace() / (double) (1L << 20))
                                + "/"
                                + String.format("%.2f", fs.getTotalSpace() / (double) (1L << 20)));
                        mv.setAvailable(true);
                        mv.setDescription(fs.getName());
                        si.add(mv);
                    }
                }
                break;
            }
            case NETWORK_INTERFACES: {
                NetworkIF[] nis = hal.getNetworkIFs();
                if (nis.length > 0) {
                    si = new ArrayList<MetricValue>();
                    for (int i = 0; i < nis.length; i++) {
                        NetworkIF ni = nis[i];
                        MetricValue mv = new MetricValue(info);
                        mv.setName(info.name() + "_" + (i + 1));
                        mv.setValue(String.format("%.2f", ni.getBytesSent() / (double) (1L << 20))
                                + "/"
                                + String.format("%.2f", ni.getBytesRecv() / (double) (1L << 20)));
                        mv.setAvailable(true);
                        mv.setDescription(ni.getName());
                        si.add(mv);
                    }
                }
                break;
            }
            case GEOSERVER_CPU_USAGE: {
                OSProcess gsProc = os.getProcess(os.getProcessId());
                double value = 100d * (gsProc.getKernelTime() + gsProc.getUserTime())
                        / gsProc.getUpTime();
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(String.format("%.2f", value));
                si = Collections.singletonList(mv);
                break;
            }
            case GEOSERVER_JVM_MEMORY_USAGE: {
                OSProcess gsProc = os.getProcess(os.getProcessId());
                double value = 100d * gsProc.getResidentSetSize() / mm.getTotal();
                MetricValue mv = new MetricValue(info);
                mv.setAvailable(true);
                mv.setValue(String.format("%.2f", value));
                si = Collections.singletonList(mv);
                break;
            }
            case TEMPERATURE: {
                double value = ss.getCpuVoltage();
                if (value > 0) {
                    MetricValue mv = new MetricValue(info);
                    mv.setAvailable(true);
                    mv.setValue(String.format("%.2f", value));
                    si = Collections.singletonList(mv);
                }
                break;
            }
            case VOLTAGE: {
                double value = ss.getCpuVoltage();
                if (value > 0) {
                    MetricValue mv = new MetricValue(info);
                    mv.setAvailable(true);
                    mv.setValue(String.format("%.2f", value));
                    si = Collections.singletonList(mv);
                }
                break;
            }
            case FAN_SPEED: {
                int[] speeds = ss.getFanSpeeds();
                if (speeds.length > 0) {
                    ArrayList<MetricValue> tmp = new ArrayList<MetricValue>(speeds.length);
                    for (int i = 0; i < speeds.length; i++) {
                        if (speeds[i] > 0) {
                            String value = String.valueOf(speeds[i]);
                            String name = info.name() + "_" + (i + 1);
                            String description = "Speed fan " + (i + 1);
                            MetricValue mv = new MetricValue(info);
                            mv.setValue(value);
                            mv.setAvailable(true);
                            mv.setDescription(description);
                            mv.setName(name);
                            tmp.add(mv);
                        }
                    }
                    if (!tmp.isEmpty()) {
                        si = tmp;
                    }
                }
                break;
            }
            default:
                break;
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return si;
    }

}
