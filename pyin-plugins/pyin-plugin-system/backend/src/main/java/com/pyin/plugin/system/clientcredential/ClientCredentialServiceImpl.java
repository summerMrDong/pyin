package com.pyin.plugin.system.clientcredential;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClientCredentialServiceImpl implements ClientCredentialService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ClientCredentialRepository clientCredentialRepository;
    private final ClientCredentialSecretCodec clientCredentialSecretCodec;

    public ClientCredentialServiceImpl(
            ClientCredentialRepository clientCredentialRepository,
            ClientCredentialSecretCodec clientCredentialSecretCodec
    ) {
        this.clientCredentialRepository = clientCredentialRepository;
        this.clientCredentialSecretCodec = clientCredentialSecretCodec;
    }

    @Override
    public List<ClientCredentialSummary> findAll(ClientCredentialQuery query) {
        LambdaQueryWrapper<ClientCredentialEntity> wrapper = new LambdaQueryWrapper<ClientCredentialEntity>()
                .orderByDesc(ClientCredentialEntity::getCreatedAt)
                .orderByAsc(ClientCredentialEntity::getId);
        if (query != null) {
            if (StringUtils.hasText(query.credentialName())) {
                wrapper.like(ClientCredentialEntity::getCredentialName, query.credentialName().trim());
            }
            if (StringUtils.hasText(query.accessKey())) {
                wrapper.like(ClientCredentialEntity::getAccessKey, query.accessKey().trim());
            }
            if (StringUtils.hasText(query.status())) {
                wrapper.eq(ClientCredentialEntity::getStatus, normalizeStatus(query.status()));
            }
        }
        return clientCredentialRepository.selectList(wrapper).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public ClientCredentialSecretResult create(CreateClientCredentialRequest request) {
        String credentialName = requireText(request == null ? null : request.getCredentialName(), "凭证名称不能为空");
        String accessKey = nextToken("cck_");
        String accessSecret = nextToken("ccs_");
        LocalDateTime now = LocalDateTime.now();

        ClientCredentialEntity entity = new ClientCredentialEntity();
        entity.setCredentialName(credentialName);
        entity.setAccessKey(accessKey);
        entity.setAccessSecretHash(clientCredentialSecretCodec.hash(accessSecret));
        entity.setAccessSecretEncrypted(clientCredentialSecretCodec.encrypt(accessSecret));
        entity.setStatus(STATUS_ENABLED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        clientCredentialRepository.insert(entity);

        return new ClientCredentialSecretResult(
                entity.getId(),
                entity.getCredentialName(),
                entity.getAccessKey(),
                accessSecret,
                entity.getStatus()
        );
    }

    @Override
    @Transactional
    public ClientCredentialSummary enable(Long id) {
        ClientCredentialEntity entity = requireCredential(id);
        entity.setStatus(STATUS_ENABLED);
        entity.setUpdatedAt(LocalDateTime.now());
        clientCredentialRepository.updateById(entity);
        return toSummary(entity);
    }

    @Override
    @Transactional
    public ClientCredentialSummary disable(Long id) {
        ClientCredentialEntity entity = requireCredential(id);
        entity.setStatus(STATUS_DISABLED);
        entity.setUpdatedAt(LocalDateTime.now());
        clientCredentialRepository.updateById(entity);
        return toSummary(entity);
    }

    @Override
    @Transactional
    public ClientCredentialSecretResult rotateSecret(Long id) {
        ClientCredentialEntity entity = requireCredential(id);
        String accessSecret = nextToken("ccs_");
        entity.setAccessSecretHash(clientCredentialSecretCodec.hash(accessSecret));
        entity.setAccessSecretEncrypted(clientCredentialSecretCodec.encrypt(accessSecret));
        entity.setUpdatedAt(LocalDateTime.now());
        clientCredentialRepository.updateById(entity);
        return new ClientCredentialSecretResult(
                entity.getId(),
                entity.getCredentialName(),
                entity.getAccessKey(),
                accessSecret,
                entity.getStatus()
        );
    }

    @Override
    public ClientCredentialEntity findByAccessKey(String accessKey) {
        if (!StringUtils.hasText(accessKey)) {
            return null;
        }
        return clientCredentialRepository.selectOne(new LambdaQueryWrapper<ClientCredentialEntity>()
                .eq(ClientCredentialEntity::getAccessKey, accessKey.trim())
                .last("LIMIT 1"));
    }

    @Override
    public ClientCredentialEntity findById(Long id) {
        return id == null ? null : clientCredentialRepository.selectById(id);
    }

    @Override
    public String decryptSecret(ClientCredentialEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getAccessSecretEncrypted())) {
            throw new IllegalArgumentException("凭证密钥不存在");
        }
        return clientCredentialSecretCodec.decrypt(entity.getAccessSecretEncrypted());
    }

    private ClientCredentialEntity requireCredential(Long id) {
        ClientCredentialEntity entity = clientCredentialRepository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("接入凭证不存在: " + id);
        }
        return entity;
    }

    private ClientCredentialSummary toSummary(ClientCredentialEntity entity) {
        return new ClientCredentialSummary(
                entity.getId(),
                entity.getCredentialName(),
                entity.getAccessKey(),
                defaultStatus(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return STATUS_ENABLED;
        }
        String normalized = value.trim().toUpperCase();
        if (!STATUS_ENABLED.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new IllegalArgumentException("不支持的凭证状态: " + value);
        }
        return normalized;
    }

    private String defaultStatus(String value) {
        return StringUtils.hasText(value) ? value : STATUS_ENABLED;
    }

    private String nextToken(String prefix) {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return prefix + value;
    }
}
