package com.pyin.plugin.system.clientcredential.controller;


import com.pyin.plugin.system.clientcredential.model.ClientCredentialQuery;
import com.pyin.plugin.system.clientcredential.model.ClientCredentialSummary;
import com.pyin.plugin.system.clientcredential.model.ClientRequestLogQuery;
import com.pyin.plugin.system.clientcredential.model.CreateClientCredentialRequest;
import com.pyin.plugin.system.clientcredential.service.ClientCredentialService;
import com.pyin.plugin.system.clientcredential.service.ClientRequestLogService;
import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@AdminMapping("/client-credentials")
public class ClientCredentialController {

    private final ClientCredentialService clientCredentialService;
    private final ClientRequestLogService clientRequestLogService;

    public ClientCredentialController(
            ClientCredentialService clientCredentialService,
            ClientRequestLogService clientRequestLogService
    ) {
        this.clientCredentialService = clientCredentialService;
        this.clientRequestLogService = clientRequestLogService;
    }

    @Permission(code = "credential:view", name = "接入凭证查看")
    @GetMapping
    public Result<List<ClientCredentialSummary>> list(
            @RequestParam(required = false) String credentialName,
            @RequestParam(required = false) String accessKey,
            @RequestParam(required = false) String status
    ) {
        return Result.ok(clientCredentialService.findAll(new ClientCredentialQuery(credentialName, accessKey, status)));
    }

    @Permission(code = "credential:create", name = "接入凭证创建")
    @PostMapping
    public Result<?> create(@RequestBody(required = false) CreateClientCredentialRequest request) {
        try {
            return Result.ok(clientCredentialService.create(request));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-400", exception.getMessage());
        }
    }

    @Permission(code = "credential:update", name = "接入凭证更新")
    @PostMapping("/{id}/enable")
    public Result<?> enable(@PathVariable Long id) {
        try {
            return Result.ok(clientCredentialService.enable(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-404", exception.getMessage());
        }
    }

    @Permission(code = "credential:update", name = "接入凭证更新")
    @PostMapping("/{id}/disable")
    public Result<?> disable(@PathVariable Long id) {
        try {
            return Result.ok(clientCredentialService.disable(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-404", exception.getMessage());
        }
    }

    @Permission(code = "credential:rotate-secret", name = "接入凭证轮换密钥")
    @PostMapping("/{id}/rotate-secret")
    public Result<?> rotateSecret(@PathVariable Long id) {
        try {
            return Result.ok(clientCredentialService.rotateSecret(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-404", exception.getMessage());
        }
    }

    @Permission(code = "credential:view-logs", name = "接入凭证查看日志")
    @GetMapping("/{id}/request-logs")
    public Result<?> requestLogs(
            @PathVariable Long id,
            @RequestParam(required = false) String requestStatus,
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String keyword
    ) {
        if (clientCredentialService.findById(id) == null) {
            return Result.fail("PYIN-CREDENTIAL-404", "接入凭证不存在: " + id);
        }
        return Result.ok(clientRequestLogService.findByCredentialId(id, new ClientRequestLogQuery(requestStatus, requestType, keyword)));
    }
}
