package com.pyin.plugin.system.audit.service;


import com.pyin.plugin.system.audit.entity.AuditLogEntity;
import java.util.List;

public interface AuditService {

    List<AuditLogEntity> recentLogs();
}
