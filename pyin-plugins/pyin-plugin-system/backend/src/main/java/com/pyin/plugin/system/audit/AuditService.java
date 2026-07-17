package com.pyin.plugin.system.audit;

import java.util.List;

public interface AuditService {

    List<AuditLogEntity> recentLogs();
}
