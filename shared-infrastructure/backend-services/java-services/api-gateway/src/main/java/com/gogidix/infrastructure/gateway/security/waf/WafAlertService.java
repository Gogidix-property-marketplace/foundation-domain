package com.gogidix.infrastructure.gateway.security.waf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WAF Alert Service.
 *
 * Sends alerts for high-risk security violations detected by the WAF.
 * Supports multiple alert channels including Slack, email, and webhook.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WafAlertService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${waf.alerts.slack.webhook-url:}")
    private String slackWebhookUrl;

    @Value("${waf.alerts.email.enabled:false}")
    private boolean emailAlertsEnabled;

    @Value("${waf.alerts.webhook.url:}")
    private String webhookUrl;

    // Rate limiting for alerts to prevent spam
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_MS = 300000; // 5 minutes

    /**
     * Sends high-risk alert for WAF violations
     */
    public void sendHighRiskAlert(WafResult wafResult) {
        if (wafResult.getRiskScore() < 8) {
            return;
        }

        String alertKey = wafResult.getClientIp() + ":" + wafResult.getReason();
        long currentTime = System.currentTimeMillis();

        // Check cooldown to prevent alert spam
        Long lastTime = lastAlertTime.get(alertKey);
        if (lastTime != null && (currentTime - lastTime) < ALERT_COOLDOWN_MS) {
            log.debug("Alert cooldown active for: {}", alertKey);
            return;
        }

        lastAlertTime.put(alertKey, currentTime);

        // Send alert to different channels
        Mono.when(
                sendSlackAlert(wafResult),
                sendEmailAlert(wafResult),
                sendWebhookAlert(wafResult)
        ).subscribe(
                null,
                error -> log.error("Failed to send WAF alert", error)
        );

        log.warn("High-risk WAF violation detected: {} from IP {} (Score: {})",
                wafResult.getReason(), wafResult.getClientIp(), wafResult.getRiskScore());
    }

    /**
     * Sends alert to Slack
     */
    private Mono<Void> sendSlackAlert(WafResult wafResult) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            return Mono.empty();
        }

        Map<String, Object> payload = createSlackPayload(wafResult);

        return Mono.fromRunnable(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

                restTemplate.postForEntity(slackWebhookUrl, entity, String.class);
                log.info("Slack alert sent for WAF violation from IP: {}", wafResult.getClientIp());
            } catch (Exception e) {
                log.error("Failed to send Slack alert", e);
            }
        });
    }

    /**
     * Sends email alert
     */
    private Mono<Void> sendEmailAlert(WafResult wafResult) {
        if (!emailAlertsEnabled) {
            return Mono.empty();
        }

        // Store email alert in queue for processing by email service
        String alertData = String.format("%s|%s|%s|%d",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                wafResult.getClientIp(),
                wafResult.getReason(),
                wafResult.getRiskScore());

        String key = "waf:alerts:email:" + System.currentTimeMillis();
        return redisTemplate.opsForList()
                .rightPush("waf:alerts:email:queue", alertData)
                .then()
                .doOnSuccess(v -> log.info("Email alert queued for WAF violation from IP: {}", wafResult.getClientIp()));
    }

    /**
     * Sends webhook alert
     */
    private Mono<Void> sendWebhookAlert(WafResult wafResult) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return Mono.empty();
        }

        Map<String, Object> payload = createWebhookPayload(wafResult);

        return Mono.fromRunnable(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

                restTemplate.postForEntity(webhookUrl, entity, String.class);
                log.info("Webhook alert sent for WAF violation from IP: {}", wafResult.getClientIp());
            } catch (Exception e) {
                log.error("Failed to send webhook alert", e);
            }
        });
    }

    /**
     * Creates Slack payload
     */
    private Map<String, Object> createSlackPayload(WafResult wafResult) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("text", "🚨 High-Risk WAF Violation Detected!");

        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", "danger");

        List<Map<String, Object>> fields = List.of(
                Map.of(
                        "title", "Client IP",
                        "value", wafResult.getClientIp(),
                        "short", true
                ),
                Map.of(
                        "title", "Violation Type",
                        "value", wafResult.getReason(),
                        "short", true
                ),
                Map.of(
                        "title", "Risk Score",
                        "value", String.valueOf(wafResult.getRiskScore()),
                        "short", true
                ),
                Map.of(
                        "title", "Timestamp",
                        "value", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "short", true
                )
        );

        if (wafResult.getDetails() != null && !wafResult.getDetails().isEmpty()) {
            fields.add(Map.of(
                    "title", "Details",
                    "value", wafResult.getDetails().toString(),
                    "short", false
            ));
        }

        attachment.put("fields", fields);
        payload.put("attachments", List.of(attachment));

        return payload;
    }

    /**
     * Creates webhook payload
     */
    private Map<String, Object> createWebhookPayload(WafResult wafResult) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "WAF_VIOLATION");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("severity", wafResult.getRiskScore() >= 9 ? "CRITICAL" : "HIGH");
        payload.put("source", "WAF");

        Map<String, Object> data = new HashMap<>();
        data.put("clientIp", wafResult.getClientIp());
        data.put("violationType", wafResult.getReason());
        data.put("riskScore", wafResult.getRiskScore());
        data.put("action", wafResult.getAction().name());
        data.put("ruleId", wafResult.getRuleId());
        data.put("requestId", wafResult.getRequestId());
        data.put("details", wafResult.getDetails());

        payload.put("data", data);

        return payload;
    }

    /**
     * Sends critical alert for DDoS attack
     */
    public void sendDdosAlert(String clientIp, int requestCount, String details) {
        WafResult ddosResult = WafResult.builder()
                .allowed(false)
                .reason("DDOS_DETECTED")
                .action(WafAction.BLOCK)
                .riskScore(10)
                .clientIp(clientIp)
                .details(Map.of(
                        "requestCount", requestCount,
                        "details", details
                ))
                .build();

        sendHighRiskAlert(ddosResult);
    }

    /**
     * Sends custom alert
     */
    public void sendCustomAlert(String message, Map<String, Object> details) {
        log.warn("Custom WAF Alert: {} - {}", message, details);

        // Store custom alert in Redis for analysis
        String key = "waf:alerts:custom:" + System.currentTimeMillis();
        redisTemplate.opsForHash()
                .putAll(key, Map.of(
                        "message", message,
                        "details", details.toString(),
                        "timestamp", LocalDateTime.now().toString()
                ))
                .then(redisTemplate.expire(key, java.time.Duration.ofDays(7)))
                .subscribe();
    }

    /**
     * Gets alert statistics
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCooldowns", lastAlertTime.size());

        // Count recent alerts from Redis
        String todayKey = "waf:alerts:" + LocalDateTime.now().toLocalDate();
        redisTemplate.opsForHash()
                .entries(todayKey)
                .doOnNext(entryMap -> {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> map = (Map<Object, Object>) entryMap;
                    stats.put("todayAlerts", map.size());
                })
                .subscribe();

        return stats;
    }

    /**
     * Clears alert cooldowns (for testing)
     */
    public void clearCooldowns() {
        lastAlertTime.clear();
        log.info("WAF alert cooldowns cleared");
    }
}