package com.gogidix.infrastructure.waf;

import java.util.List;
import java.util.Map;

public class WafRule {
    private String id;
    private String name;
    private String description;
    private WafAction action;
    private List<String> conditions;
    private Map<String, Object> parameters;

    public WafRule() {}

    public WafRule(String id, String name, WafAction action) {
        this.id = id;
        this.name = name;
        this.action = action;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public WafAction getAction() { return action; }
    public void setAction(WafAction action) { this.action = action; }

    public List<String> getConditions() { return conditions; }
    public void setConditions(List<String> conditions) { this.conditions = conditions; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}
