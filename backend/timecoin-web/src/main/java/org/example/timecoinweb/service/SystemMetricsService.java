package org.example.timecoinweb.service;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 采集「运行本后端进程」所在机器的系统指标（管理员查看）。
 */
@Slf4j
@Service
public class SystemMetricsService {

    public Map<String, Object> snapshot() {
        Map<String, Object> root = new HashMap<>();
        root.put("collectedAt", Instant.now().toString());

        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        HardwareAbstractionLayer hal = si.getHardware();
        CentralProcessor proc = hal.getProcessor();
        GlobalMemory mem = hal.getMemory();

        Map<String, Object> server = new HashMap<>();
        server.put("hostname", safe(os.getNetworkParams().getHostName()));
        server.put("osName", safe(os.toString()));
        server.put("osFamily", safe(os.getFamily()));
        server.put("osVersion", safe(os.getVersionInfo().getVersion()));
        server.put("arch", System.getProperty("os.arch", ""));
        server.put("uptimeSeconds", os.getSystemUptime());
        server.put("primaryIpv4s", collectIpv4s(hal));
        root.put("server", server);

        Map<String, Object> cpu = new HashMap<>();
        cpu.put("physicalCores", proc.getPhysicalProcessorCount());
        cpu.put("logicalCores", proc.getLogicalProcessorCount());
        long[] prev = proc.getSystemCpuLoadTicks();
        try {
            Thread.sleep(280);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        double load = proc.getSystemCpuLoadBetweenTicks(prev);
        if (load < 0) {
            cpu.put("usagePercent", null);
        } else {
            cpu.put("usagePercent", round2(load * 100.0));
        }
        root.put("cpu", cpu);

        long totalMem = mem.getTotal();
        long availMem = mem.getAvailable();
        long usedMem = totalMem - availMem;
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalBytes", totalMem);
        memory.put("availableBytes", availMem);
        memory.put("usedBytes", usedMem);
        memory.put("usagePercent", totalMem > 0 ? round2(usedMem * 100.0 / totalMem) : 0.0);
        root.put("memory", memory);

        List<Map<String, Object>> disks = new ArrayList<>();
        for (OSFileStore store : os.getFileSystem().getFileStores()) {
            long total = store.getTotalSpace();
            if (total <= 0) {
                continue;
            }
            long usable = store.getUsableSpace();
            long used = total - usable;
            Map<String, Object> row = new HashMap<>();
            row.put("mount", safe(store.getMount()));
            row.put("label", safe(store.getDescription()));
            row.put("fsType", safe(store.getType()));
            row.put("totalBytes", total);
            row.put("usableBytes", usable);
            row.put("usedBytes", used);
            row.put("usedPercent", round2(used * 100.0 / total));
            disks.add(row);
        }
        root.put("disks", disks);

        Runtime rt = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("javaVersion", System.getProperty("java.version", ""));
        jvm.put("maxMemoryBytes", rt.maxMemory());
        jvm.put("totalMemoryBytes", rt.totalMemory());
        jvm.put("freeMemoryBytes", rt.freeMemory());
        root.put("jvm", jvm);

        return root;
    }

    private static List<String> collectIpv4s(HardwareAbstractionLayer hal) {
        Set<String> ips = new LinkedHashSet<>();
        try {
            // 无参 getNetworkIFs() 在 OSHI 中表示「非本地」网卡，已排除回环
            for (NetworkIF nif : hal.getNetworkIFs()) {
                if (nif == null) {
                    continue;
                }
                String[] v4 = nif.getIPv4addr();
                if (v4 == null) {
                    continue;
                }
                for (String ip : v4) {
                    if (ip != null && !ip.isEmpty() && !"127.0.0.1".equals(ip)) {
                        ips.add(ip);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("枚举网卡 IP 失败", e);
        }
        return new ArrayList<>(ips);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
