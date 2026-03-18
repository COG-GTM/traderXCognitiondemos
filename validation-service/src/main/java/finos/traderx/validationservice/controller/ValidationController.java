package finos.traderx.validationservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.validationservice.model.ValidationRequest;
import finos.traderx.validationservice.model.ValidationResult;
import finos.traderx.validationservice.service.AccountServiceClient;
import finos.traderx.validationservice.service.ReferenceDataClient;
import io.swagger.v3.oas.annotations.Operation;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/validate", produces = "application/json")
public class ValidationController {

    @Autowired
    private ReferenceDataClient referenceDataClient;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Operation(description = "Validate a trade order's security and account")
    @PostMapping("/trade")
    public ResponseEntity<ValidationResult> validateTrade(@RequestBody ValidationRequest request) {
        if (!referenceDataClient.validateTicker(request.getSecurity())) {
            return ResponseEntity.ok(ValidationResult.failure(request.getSecurity() + " not found in Reference data service."));
        }
        if (!accountServiceClient.validateAccount(request.getAccountId())) {
            return ResponseEntity.ok(ValidationResult.failure(request.getAccountId() + " not found in Account service."));
        }
        return ResponseEntity.ok(ValidationResult.success());
    }
}
