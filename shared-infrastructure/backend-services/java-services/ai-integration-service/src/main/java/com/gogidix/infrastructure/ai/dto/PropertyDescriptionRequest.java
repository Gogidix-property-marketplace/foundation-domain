package com.gogidix.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Request DTO for Property Description Generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDescriptionRequest {

    @NotBlank(message = "Property ID is required")
    @Size(max = 100, message = "Property ID must not exceed 100 characters")
    @JsonProperty("propertyId")
    private String propertyId;

    @NotBlank(message = "Property type is required")
    @Pattern(regexp = "^(APARTMENT|HOUSE|VILLA|CONDO|TOWNHOUSE|STUDIO|PENTHOUSE|LOFT|COMMERCIAL_OFFICE|RETAIL_SPACE|WAREHOUSE|LAND)$",
             message = "Invalid property type")
    @JsonProperty("propertyType")
    private String propertyType;

    @NotBlank(message = "Property category is required")
    @Pattern(regexp = "^(RESIDENTIAL|COMMERCIAL|LUXURY|INVESTMENT|VACATION|NEW_CONSTRUCTION)$",
             message = "Invalid property category")
    @JsonProperty("propertyCategory")
    private String propertyCategory;

    @Min(value = 0, message = "Bedrooms cannot be negative")
    @Max(value = 20, message = "Bedrooms cannot exceed 20")
    @JsonProperty("bedrooms")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms cannot be negative")
    @Max(value = 20, message = "Bathrooms cannot exceed 20")
    @JsonProperty("bathrooms")
    private Integer bathrooms;

    @Min(value = 100, message = "Square footage must be at least 100")
    @Max(value = 50000, message = "Square footage cannot exceed 50,000")
    @JsonProperty("squareFootage")
    private Integer squareFootage;

    @NotBlank(message = "Location is required")
    @Size(min = 5, max = 200, message = "Location must be between 5 and 200 characters")
    @JsonProperty("location")
    private String location;

    @NotNull(message = "Features list cannot be null")
    @Size(min = 1, max = 50, message = "Must provide between 1 and 50 features")
    @JsonProperty("features")
    private List<@NotBlank(message = "Feature cannot be blank") String> features;

    @Size(max = 20, message = "Unique selling points cannot exceed 20 items")
    @JsonProperty("uniqueSellingPoints")
    private List<String> uniqueSellingPoints;

    @Size(max = 20, message = "Nearby amenities cannot exceed 20 items")
    @JsonProperty("nearbyAmenities")
    private List<String> nearbyAmenities;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Invalid price format")
    @JsonProperty("price")
    private Double price;

    @JsonProperty("priceCurrency")
    private String priceCurrency = "USD";

    @Min(value = 1900, message = "Year built must be after 1900")
    @Max(value = 2100, message = "Year built cannot be in the future")
    @JsonProperty("yearBuilt")
    private Integer yearBuilt;

    @JsonProperty("isNewConstruction")
    private Boolean isNewConstruction = false;

    @JsonProperty("isRenovated")
    private Boolean isRenovated = false;

    @JsonProperty("renovationYear")
    private Integer renovationYear;

    @Size(max = 500, message = "Additional notes cannot exceed 500 characters")
    @JsonProperty("additionalNotes")
    private String additionalNotes;

    // Target audience for the description
    @JsonProperty("targetAudience")
    private TargetAudience targetAudience = TargetAudience.GENERAL;

    @JsonProperty("tone")
    private DescriptionTone tone = DescriptionTone.PROFESSIONAL;

    @JsonProperty("language")
    private String language = "en";

    @JsonProperty("includeSeoKeywords")
    private Boolean includeSeoKeywords = true;

    @JsonProperty("includeCallToAction")
    private Boolean includeCallToAction = true;

    /**
     * Target audience enum for property descriptions
     */
    public enum TargetAudience {
        GENERAL,
        FIRST_TIME_BUYERS,
        INVESTORS,
        FAMILIES,
        YOUNG_PROFESSIONALS,
        RETIREES,
        LUXURY_BUYERS,
        STUDENTS,
        COMMERCIAL_TENANTS
    }

    /**
     * Description tone enum
     */
    public enum DescriptionTone {
        PROFESSIONAL,
        FRIENDLY,
        LUXURY,
        CASUAL,
        FORMAL,
        ENTREPRENEURIAL,
        FAMILY_FOCUSED,
        MODERN
    }
}