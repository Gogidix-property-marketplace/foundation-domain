package com.gogidix.microservices.advanced.controller;

import com.gogidix.microservices.advanced.service.QuantumComputingAIService;
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
 * REST Controller for Quantum Computing Analysis AI Service Operations
 */
@RestController
@RequestMapping("/api/v1/quantum-computing-ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quantum Computing AI API", description = "Quantum Computing Analysis AI Service Operations")
public class QuantumComputingAIController {

    private final QuantumComputingAIService quantumComputingAIService;

    @PostMapping("/portfolio/optimize")
    @Operation(summary = "Optimize portfolio using quantum annealing", description = "Solve portfolio optimization using quantum annealing algorithms")
    @PreAuthorize("hasRole('PORTFOLIO_MANAGER') or hasRole('QUANTUM_ANALYST')")
    public CompletableFuture<ResponseEntity<QuantumPortfolioOptimizationResultDto>> optimizePortfolio(
            @Valid @RequestBody QuantumPortfolioOptimizationRequestDto request) {
        log.info("Starting quantum portfolio optimization for {} properties", request.getPropertyIds().size());
        request.setStartTime(System.currentTimeMillis());
        return quantumComputingAIService.optimizePortfolioQuantum(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error optimizing portfolio with quantum computing", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/market/predict")
    @Operation(summary = "Predict market using quantum ML", description = "Perform quantum-enhanced market prediction with quantum machine learning")
    @PreAuthorize("hasRole('MARKET_ANALYST') or hasRole('QUANTUM_ANALYST')")
    public CompletableFuture<ResponseEntity<QuantumMarketPredictionDto>> predictMarket(
            @Valid @RequestBody QuantumMarketPredictionRequestDto request) {
        log.info("Starting quantum market prediction for location: {}", request.getLocation());
        return quantumComputingAIService.predictMarketQuantum(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error predicting market with quantum computing", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/investment/analyze")
    @Operation(summary = "Analyze investment using quantum algorithms", description = "Solve real estate investment problems using quantum optimization")
    @PreAuthorize("hasRole('INVESTMENT_ANALYST') or hasRole('QUANTUM_ANALYST')")
    public CompletableFuture<ResponseEntity<QuantumInvestmentAnalysisDto>> analyzeInvestment(
            @Valid @RequestBody QuantumInvestmentAnalysisRequestDto request) {
        log.info("Starting quantum investment analysis for budget: {}", request.getBudget());
        return quantumComputingAIService.analyzeInvestmentQuantum(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error analyzing investment with quantum computing", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/risk/assess")
    @Operation(summary = "Assess risk using quantum simulation", description = "Perform quantum-enhanced risk assessment with quantum Monte Carlo")
    @PreAuthorize("hasRole('RISK_ANALYST') or hasRole('QUANTUM_ANALYST')")
    public CompletableFuture<ResponseEntity<QuantumRiskAssessmentDto>> assessRisk(
            @Valid @RequestBody QuantumRiskAssessmentRequestDto request) {
        log.info("Starting quantum risk assessment for {} properties", request.getPropertyIds().size());
        return quantumComputingAIService.assessRiskQuantum(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error assessing risk with quantum computing", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/development/optimize")
    @Operation(summary = "Optimize development using quantum algorithms", description = "Optimize real estate development using quantum resource allocation")
    @PreAuthorize("hasRole('DEVELOPMENT_MANAGER') or hasRole('QUANTUM_ANALYST')")
    public CompletableFuture<ResponseEntity<QuantumDevelopmentOptimizationDto>> optimizeDevelopment(
            @Valid @RequestBody QuantumDevelopmentOptimizationRequestDto request) {
        log.info("Starting quantum development optimization for project: {}", request.getProjectId());
        return quantumComputingAIService.optimizeDevelopmentQuantum(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error optimizing development with quantum computing", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/performance/analyze")
    @Operation(summary = "Analyze quantum algorithm performance", description = "Analyze and benchmark quantum algorithm performance")
    @PreAuthorize("hasRole('QUANTUM_ANALYST') or hasRole('SYSTEM_ADMIN')")
    public CompletableFuture<ResponseEntity<QuantumPerformanceAnalysisDto>> analyzePerformance(
            @Valid @RequestBody QuantumPerformanceAnalysisRequestDto request) {
        log.info("Starting quantum performance analysis for algorithm: {}", request.getAlgorithmType());
        return quantumComputingAIService.analyzeQuantumPerformance(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error analyzing quantum performance", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/algorithms")
    @Operation(summary = "Get available quantum algorithms", description = "List all available quantum algorithms and their capabilities")
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<Object> getAvailableAlgorithms() {
        log.info("Fetching available quantum algorithms");
        return ResponseEntity.ok(java.util.Map.of(
            "algorithms", java.util.Arrays.asList(
                "Quantum Approximate Optimization Algorithm (QAOA)",
                "Quantum Annealing",
                "Quantum Neural Networks",
                "Quantum Support Vector Machines",
                "Quantum Monte Carlo",
                "Variational Quantum Eigensolver (VQE)",
                "Quantum Phase Estimation",
                "Quantum Fourier Transform"
            ),
            "hardware", java.util.Arrays.asList(
                "IBM Quantum System One",
                "Google Sycamore",
                "IonQ Quantum Computer",
                "D-Wave Advantage System",
                "Rigetti Quantum Cloud Services"
            ),
            "capabilities", java.util.Arrays.asList(
                "Portfolio Optimization",
                "Market Prediction",
                "Risk Assessment",
                "Investment Analysis",
                "Development Optimization",
                "Resource Allocation",
                "Monte Carlo Simulation"
            )
        ));
    }
}