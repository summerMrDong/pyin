package com.pyin.plugin.system.clientcredential;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ClientRequestLogServiceImpl implements ClientRequestLogService {

    private final ClientRequestLogRepository clientRequestLogRepository;
    private final ClientCredentialRepository clientCredentialRepository;

    public ClientRequestLogServiceImpl(
            ClientRequestLogRepository clientRequestLogRepository,
            ClientCredentialRepository clientCredentialRepository
    ) {
        this.clientRequestLogRepository = clientRequestLogRepository;
        this.clientCredentialRepository = clientCredentialRepository;
    }

    @Override
    public void log(
            Long credentialId,
            String accessKey,
            String requestType,
            String requestUri,
            String httpMethod,
            String clientIp,
            String requestStatus,
            String failureCode,
            String failureMessage
    ) {
        ClientRequestLogEntity entity = new ClientRequestLogEntity();
        entity.setCredentialId(credentialId);
        entity.setAccessKey(trimToNull(accessKey));
        entity.setRequestType(trimToNull(requestType));
        entity.setRequestUri(trimToNull(requestUri));
        entity.setHttpMethod(trimToNull(httpMethod));
        entity.setClientIp(trimToNull(clientIp));
        entity.setRequestStatus(normalizeStatus(requestStatus));
        entity.setFailureCode(trimToNull(failureCode));
        entity.setFailureMessage(trimToNull(failureMessage));
        entity.setCreatedAt(LocalDateTime.now());
        clientRequestLogRepository.insert(entity);
    }

    @Override
    public List<ClientRequestLogView> findByCredentialId(Long credentialId, ClientRequestLogQuery query) {
        LambdaQueryWrapper<ClientRequestLogEntity> wrapper = new LambdaQueryWrapper<ClientRequestLogEntity>()
                .eq(ClientRequestLogEntity::getCredentialId, credentialId)
                .orderByDesc(ClientRequestLogEntity::getCreatedAt)
                .orderByDesc(ClientRequestLogEntity::getId);
        if (query != null) {
            if (StringUtils.hasText(query.requestStatus())) {
                wrapper.eq(ClientRequestLogEntity::getRequestStatus, normalizeStatus(query.requestStatus()));
            }
            if (StringUtils.hasText(query.requestType())) {
                wrapper.eq(ClientRequestLogEntity::getRequestType, query.requestType().trim().toUpperCase());
            }
            if (StringUtils.hasText(query.keyword())) {
                wrapper.like(ClientRequestLogEntity::getRequestUri, query.keyword().trim());
            }
        }
        List<ClientRequestLogEntity> logs = clientRequestLogRepository.selectList(wrapper);
        Map<Long, ClientCredentialEntity> credentialMap = clientCredentialRepository.selectBatchIds(logs.stream()
                        .map(ClientRequestLogEntity::getCredentialId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(ClientCredentialEntity::getId, Function.identity()));
        return logs.stream()
                .map(log -> {
                    ClientCredentialEntity credential = credentialMap.get(log.getCredentialId());
                    return new ClientRequestLogView(
                            log.getId(),
                            log.getCredentialId(),
                            credential == null ? null : credential.getCredentialName(),
                            log.getAccessKey(),
                            log.getRequestType(),
                            log.getRequestUri(),
                            log.getHttpMethod(),
                            log.getClientIp(),
                            log.getRequestStatus(),
                            log.getFailureCode(),
                            log.getFailureMessage(),
                            log.getCreatedAt()
                    );
                })
                .toList();
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return STATUS_SUCCESS;
        }
        String normalized = value.trim().toUpperCase();
        if (STATUS_SUCCESS.equals(normalized) || STATUS_FAILED.equals(normalized)) {
            return normalized;
        }
        return STATUS_FAILED;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
