package com.pyin.plugin.exportworkshop.support;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("pyin.center.export-workshop")
public class ExportWorkshopProperties {

    private String storageRoot = "./pyin-distribution-parent/runtime/pyin-config-center-runtime/data/plugin-data/export-workshop";
    private long maxFileSizeBytes = 20 * 1024 * 1024L;
    private List<String> localRoots = new ArrayList<>();

    public String getStorageRoot() { return storageRoot; }
    public void setStorageRoot(String storageRoot) { this.storageRoot = storageRoot; }
    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }
    public List<String> getLocalRoots() { return localRoots; }
    public void setLocalRoots(List<String> localRoots) { this.localRoots = localRoots == null ? new ArrayList<>() : localRoots; }
}
