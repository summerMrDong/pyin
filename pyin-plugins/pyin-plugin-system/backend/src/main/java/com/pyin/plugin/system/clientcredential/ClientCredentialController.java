package com.pyin.plugin.system.clientcredential;

import com.pyin.plugin.common.api.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/client-credentials")
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

    @GetMapping
    public Result<List<ClientCredentialSummary>> list(
            @RequestParam(required = false) String credentialName,
            @RequestParam(required = false) String accessKey,
            @RequestParam(required = false) String status
    ) {
        return Result.ok(clientCredentialService.findAll(new ClientCredentialQuery(credentialName, accessKey, status)));
    }

    @PostMapping
    public Result<?> create(@RequestBody(required = false) CreateClientCredentialRequest request) {
        try {
            return Result.ok(clientCredentialService.create(request));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-400", exception.getMessage());
        }
    }

    @PostMapping("/{id}/enable")
    public Result<?> enable(@PathVariable Long id) {
        try {
            return Result.ok(clientCredentialService.enable(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-404", exception.getMessage());
        }
    }

    @PostMapping("/{id}/disable")
    public Result<?> disable(@PathVariable Long id) {
        try {
            return Result.ok(clientCredentialService.disable(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-404", exception.getMessage());
        }
    }

    @PostMapping("/{id}/rotate-secret")
    public Result<?> rotateSecret(@PathVariable Long id) {
        try {
            return Result.ok(clientCredentialService.rotateSecret(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-CREDENTIAL-404", exception.getMessage());
        }
    }

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
