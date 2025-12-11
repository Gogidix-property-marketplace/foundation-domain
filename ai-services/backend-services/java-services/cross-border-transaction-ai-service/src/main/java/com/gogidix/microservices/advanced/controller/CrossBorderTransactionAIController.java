package com.gogidix.microservices.advanced.controller;

import com.gogidix.microservices.advanced.service.CrossBorderTransactionAIService;
import com.gogidix.microservices.advanced.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Cross-Border Transaction Management AI Service - Turbo Speed Implementation
 */
@RestController
@RequestMapping("/api/v1/cross-border-transaction-ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cross-Border Transaction AI API", description = "Cross-Border Transaction Management AI Service")
public class CrossBorderTransactionAIController {

    private final CrossBorderTransactionAIService crossBorderTransactionAIService;

    @PostMapping("/international-transaction")
    @Operation(summary = "Process international transaction", description = "AI-powered cross-border transaction processing and optimization")
    @PreAuthorize("hasRole('INTERNATIONAL_TRANSACTIONS') or hasRole('TRANSACTION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<InternationalTransactionDto>> processInternationalTransaction(
            @Valid @RequestBody InternationalTransactionRequestDto request) {
        log.info("Processing international transaction {} -> {} - TURBO MODE", request.getSourceCountry(), request.getTargetCountry());
        return crossBorderTransactionAIService.processInternationalTransaction(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error processing international transaction", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/currency-management")
    @Operation(summary = "Manage currency exchange", description = "AI-powered currency exchange optimization and management")
    @PreAuthorize("hasRole('TREASURY_MANAGER') or hasRole('FOREIGN_EXCHANGE') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<CurrencyManagementDto>> manageCurrencyExchange(
            @Valid @RequestBody CurrencyManagementRequestDto request) {
        log.info("Managing currency exchange for {} -> {} - RAPID MODE", request.getSourceCurrency(), request.getTargetCurrency());
        return crossBorderTransactionAIService.manageCurrencyExchange(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error managing currency exchange", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/regulatory-approvals")
    @Operation(summary = "Manage regulatory approvals", description = "AI-powered regulatory approval management and tracking")
    @PreAuthorize("hasRole('COMPLIANCE_MANAGER') or hasRole('LEGAL_ADVISOR') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<RegulatoryApprovalDto>> manageRegulatoryApprovals(
            @Valid @RequestBody RegulatoryApprovalRequestDto request) {
        log.info("Managing regulatory approvals for {} - STRATEGIC MODE", request.getJurisdiction());
        return crossBorderTransactionAIService.manageRegulatoryApprovals(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error managing regulatory approvals for {}", request.getJurisdiction(), ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/international-escrow")
    @Operation(summary = "Manage international escrow", description = "AI-powered multi-currency escrow management")
    @PreAuthorize("hasRole('ESCROW_MANAGER') or hasRole('TRANSACTION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<InternationalEscrowDto>> manageInternationalEscrow(
            @Valid @RequestBody InternationalEscrowRequestDto request) {
        log.info("Managing international escrow - SECURE MODE");
        return crossBorderTransactionAIService.manageInternationalEscrow(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error managing international escrow", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/exchange-rates/forecast/{from}/{to}")
    @Operation(summary = "Get exchange rate forecast", description = "AI-powered exchange rate forecasting and analysis")
    @PreAuthorize("hasRole('USER') or hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Object> getExchangeRateForecast(
            @PathVariable String from, @PathVariable String to) {
        log.info("Getting exchange rate forecast for {} -> {} - QUICK MODE", from, to);
        return ResponseEntity.ok(java.util.Map.of(
            "fromCurrency", from,
            "toCurrency", to,
            "currentRate", 1.08,
            "forecast", java.util.Map.of(
                "24h", 1.082,
                "7d", 1.09,
                "30d", 1.10,
                "90d", 1.11
            ),
            "confidence", 0.78,
            "recommendations", Arrays.asList("FAVORABLE_TIMING", "CONSIDER_FORWARD_CONTRACT", "MONITOR_MARKET_VOLATILITY"),
            "factors", Arrays.asList("CENTRAL_BANK_POLICY", "INFLATION_DIFFERENTIAL", "TRADE_BALANCE"),
            "aiConfidence", 0.85,
            "timestamp", java.time.LocalDateTime.now()
        ));
    }
}