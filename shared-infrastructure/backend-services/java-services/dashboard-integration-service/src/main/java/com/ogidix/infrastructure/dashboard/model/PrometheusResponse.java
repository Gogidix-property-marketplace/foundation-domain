package com.ogidix.infrastructure.dashboard.model;

import java.util.List;
import java.util.Map;

/**
 * Prometheus Response model for Dashboard Integration
 * Represents the structure of Prometheus API responses
 */
public class PrometheusResponse {

    private String status;
    private PrometheusData data;

    public PrometheusResponse() {}

    public PrometheusResponse(String status, PrometheusData data) {
        this.status = status;
        this.data = data;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PrometheusData getData() {
        return data;
    }

    public void setData(PrometheusData data) {
        this.data = data;
    }

    public static class PrometheusData {
        private String resultType;
        private List<Result> result;

        public PrometheusData() {}

        public PrometheusData(String resultType, List<Result> result) {
            this.resultType = resultType;
            this.result = result;
        }

        public String getResultType() {
            return resultType;
        }

        public void setResultType(String resultType) {
            this.resultType = resultType;
        }

        public List<Result> getResult() {
            return result;
        }

        public void setResult(List<Result> result) {
            this.result = result;
        }
    }

    public static class Result {
        private Map<String, String> metric;
        private List<String> value;

        public Result() {}

        public Result(Map<String, String> metric, List<String> value) {
            this.metric = metric;
            this.value = value;
        }

        public Map<String, String> getMetric() {
            return metric;
        }

        public void setMetric(Map<String, String> metric) {
            this.metric = metric;
        }

        public List<String> getValue() {
            return value;
        }

        public void setValue(List<String> value) {
            this.value = value;
        }
    }

    @Override
    public String toString() {
        return "PrometheusResponse{" +
                "status='" + status + '\'' +
                ", data=" + data +
                '}';
    }
}