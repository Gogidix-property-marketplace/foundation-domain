"""
Property Image Analysis Service

CNN-based computer vision for property image analysis,
room detection, feature extraction, and quality assessment.
"""

import cv2
import numpy as np
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from torchvision import models
from PIL import Image
import logging
from typing import Dict, List, Optional, Tuple, Any, Union
from pathlib import Path
import joblib
import json
from datetime import datetime

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger

logger = get_logger(__name__)


class PropertyImageAnalyzer:
    """
    Advanced property image analyzer using CNNs and computer vision.
    Performs room detection, quality assessment, and feature extraction.
    """

    def __init__(self, model_path: Optional[str] = None):
        """Initialize the image analyzer."""
        self.settings = get_settings()
        self.model_path = model_path or self.settings.IMAGE_RECOGNITION_MODEL_PATH
        self.device = torch.device("cuda" if torch.settings.is_gpu_available() else "cpu")

        # Initialize models
        self.room_classifier = None
        self.feature_extractor = None
        self.quality_assessor = None
        self.object_detector = None

        # Image transforms
        self.transform = transforms.Compose([
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(
                mean=[0.485, 0.456, 0.406],
                std=[0.229, 0.224, 0.225]
            )
        ])

        # Room types
        self.room_types = [
            "living_room",
            "bedroom",
            "kitchen",
            "bathroom",
            "dining_room",
            "office",
            "garage",
            "outdoor",
            "balcony",
            "hallway",
            "closet",
            "laundry"
        ]

        # Property features to detect
        self.property_features = {
            "interior": [
                "hardwood_floor",
                "carpet_floor",
                "tile_floor",
                "granite_countertop",
                "stainless_steel_appliances",
                "fireplace",
                "ceiling_fan",
                "crown_molding",
                "bay_window",
                "walk_in_closet",
                "island_kitchen",
                "pantry",
                "vaulted_ceiling",
                "recessed_lighting"
            ],
            "exterior": [
                "garage",
                "pool",
                "garden",
                "patio",
                "deck",
                "fence",
                "driveway",
                "lawn",
                "trees",
                "solar_panels",
                "sprinkler_system",
                "outdoor_lighting"
            ],
            "views": [
                "mountain_view",
                "ocean_view",
                "city_view",
                "lake_view",
                "park_view",
                "golf_view"
            ]
        }

        self._initialize_models()

    def _initialize_models(self):
        """Initialize the computer vision models."""
        # Room classification model
        self.room_classifier = RoomClassifier(
            num_classes=len(self.room_types)
        ).to(self.device)

        # Feature extractor (pre-trained ResNet)
        self.feature_extractor = models.resnet50(pretrained=True)
        self.feature_extractor.fc = nn.Identity()  # Remove classification layer
        self.feature_extractor = self.feature_extractor.to(self.device)
        self.feature_extractor.eval()

        # Image quality assessment model
        self.quality_assessor = ImageQualityAssessor().to(self.device)

        # Object detector (YOLO-based)
        self.object_detector = PropertyObjectDetector()

    def load_model(self):
        """Load trained models from disk."""
        try:
            model_path = Path(self.model_path)
            if not model_path.exists():
                raise FileNotFoundError(f"Model path {self.model_path} does not exist")

            # Load room classifier
            room_checkpoint = torch.load(
                model_path / "room_classifier.pth",
                map_location=self.device
            )
            self.room_classifier.load_state_dict(room_checkpoint["model_state_dict"])
            self.room_classifier.eval()

            # Load quality assessor
            quality_checkpoint = torch.load(
                model_path / "quality_assessor.pth",
                map_location=self.device
            )
            self.quality_assessor.load_state_dict(quality_checkpoint["model_state_dict"])
            self.quality_assessor.eval()

            # Load object detector
            self.object_detector.load_model(model_path / "object_detector")

            # Load feature encoders
            if (model_path / "feature_encoders.pkl").exists():
                self.feature_encoders = joblib.load(
                    model_path / "feature_encoders.pkl"
                )

            logger.info(
                "Image analysis models loaded successfully",
                path=self.model_path,
                device=str(self.device)
            )

        except Exception as e:
            logger.error(
                "Failed to load image models",
                error=str(e),
                path=self.model_path
            )
            raise

    def save_model(self):
        """Save trained models to disk."""
        try:
            model_path = Path(self.model_path)
            model_path.mkdir(parents=True, exist_ok=True)

            # Save room classifier
            torch.save(
                {"model_state_dict": self.room_classifier.state_dict()},
                model_path / "room_classifier.pth"
            )

            # Save quality assessor
            torch.save(
                {"model_state_dict": self.quality_assessor.state_dict()},
                model_path / "quality_assessor.pth"
            )

            # Save object detector
            self.object_detector.save_model(model_path / "object_detector")

            # Save feature encoders
            if hasattr(self, "feature_encoders"):
                joblib.dump(
                    self.feature_encoders,
                    model_path / "feature_encoders.pkl"
                )

            logger.info(
                "Image analysis models saved successfully",
                path=self.model_path
            )

        except Exception as e:
            logger.error(
                "Failed to save image models",
                error=str(e),
                path=self.model_path
            )
            raise

    def analyze_image(
        self,
        image_path: Union[str, Path],
        include_detailed_features: bool = True,
        include_objects: bool = True
    ) -> Dict[str, Any]:
        """
        Analyze a property image comprehensively.

        Args:
            image_path: Path to the image file
            include_detailed_features: Whether to include detailed feature analysis
            include_objects: Whether to include object detection

        Returns:
            Dictionary containing analysis results
        """
        try:
            # Load and preprocess image
            image = self._load_image(image_path)
            original_size = image.size

            # Basic image information
            analysis = {
                "image_path": str(image_path),
                "timestamp": datetime.now().isoformat(),
                "image_size": {
                    "width": original_size[0],
                    "height": original_size[1]
                },
                "aspect_ratio": original_size[0] / original_size[1]
            }

            # Room classification
            analysis["room_classification"] = self._classify_room(image)

            # Image quality assessment
            analysis["quality_assessment"] = self._assess_quality(image)

            # Extract features
            analysis["features"] = self._extract_features(image, include_detailed_features)

            # Object detection
            if include_objects:
                analysis["objects"] = self._detect_objects(image)

            # Aesthetic analysis
            analysis["aesthetics"] = self._analyze_aesthetics(image)

            # Layout analysis
            analysis["layout"] = self._analyze_layout(image)

            return analysis

        except Exception as e:
            logger.error(
                "Image analysis failed",
                error=str(e),
                image_path=str(image_path)
            )
            raise

    def analyze_multiple_images(
        self,
        image_paths: List[Union[str, Path]],
        batch_processing: bool = True
    ) -> List[Dict[str, Any]]:
        """
        Analyze multiple property images.

        Args:
            image_paths: List of image file paths
            batch_processing: Whether to use batch processing for efficiency

        Returns:
            List of analysis results for each image
        """
        results = []

        if batch_processing:
            # Process in batches for efficiency
            batch_size = 8
            for i in range(0, len(image_paths), batch_size):
                batch_paths = image_paths[i:i + batch_size]
                batch_results = self._process_batch(batch_paths)
                results.extend(batch_results)
        else:
            # Process individually
            for image_path in image_paths:
                result = self.analyze_image(image_path)
                results.append(result)

        return results

    def _load_image(self, image_path: Union[str, Path]) -> Image.Image:
        """Load and validate image."""
        image = Image.open(image_path).convert("RGB")

        # Validate image size
        if image.size[0] < 224 or image.size[1] < 224:
            logger.warning(
                "Image too small, resizing to minimum",
                path=str(image_path),
                size=image.size
            )
            image = image.resize((224, 224), Image.LANCZOS)

        return image

    def _classify_room(self, image: Image.Image) -> Dict[str, Any]:
        """Classify the room type from the image."""
        try:
            # Preprocess image
            input_tensor = self.transform(image).unsqueeze(0).to(self.device)

            # Get prediction
            with torch.no_grad():
                outputs = self.room_classifier(input_tensor)
                probabilities = torch.softmax(outputs, dim=1)
                confidence, predicted = torch.max(probabilities, 1)

            # Create result
            result = {
                "predicted_room": self.room_types[predicted.item()],
                "confidence": confidence.item(),
                "probabilities": {
                    self.room_types[i]: prob.item()
                    for i, prob in enumerate(probabilities[0])
                }
            }

            # Add top 3 predictions
            top3_probs, top3_indices = torch.topk(probabilities[0], 3)
            result["top_predictions"] = [
                {
                    "room": self.room_types[idx.item()],
                    "probability": prob.item(),
                    "confidence": (prob.item() / confidence.item()) * 100
                }
                for idx, prob in zip(top3_indices, top3_probs)
            ]

            return result

        except Exception as e:
            logger.error(
                "Room classification failed",
                error=str(e)
            )
            return {
                "predicted_room": "unknown",
                "confidence": 0.0,
                "probabilities": {},
                "error": str(e)
            }

    def _assess_quality(self, image: Image.Image) -> Dict[str, Any]:
        """Assess image quality metrics."""
        try:
            # Convert to numpy array
            img_array = np.array(image)

            # Calculate quality metrics
            metrics = {
                "brightness": self._calculate_brightness(img_array),
                "contrast": self._calculate_contrast(img_array),
                "sharpness": self._calculate_sharpness(img_array),
                "noise": self._calculate_noise(img_array),
                "resolution": self._calculate_resolution(img_array),
                "composition_score": self._calculate_composition_score(img_array),
                "color_balance": self._calculate_color_balance(img_array)
            }

            # Overall quality score (0-100)
            quality_score = (
                metrics["brightness"] * 0.15 +
                metrics["contrast"] * 0.15 +
                metrics["sharpness"] * 0.25 +
                (100 - metrics["noise"]) * 0.15 +
                metrics["resolution"] * 0.15 +
                metrics["composition_score"] * 0.15
            )

            # Quality rating
            if quality_score >= 90:
                quality_rating = "excellent"
            elif quality_score >= 75:
                quality_rating = "good"
            elif quality_score >= 60:
                quality_rating = "fair"
            else:
                quality_rating = "poor"

            # NN-based quality assessment
            input_tensor = self.transform(image).unsqueeze(0).to(self.device)
            with torch.no_grad():
                nn_quality = self.quality_assessor(input_tensor).item() * 100

            # Combine scores
            final_score = (quality_score * 0.6 + nn_quality * 0.4)

            return {
                "overall_score": round(final_score, 2),
                "rating": quality_rating,
                "metrics": metrics,
                "nn_score": round(nn_quality, 2)
            }

        except Exception as e:
            logger.error(
                "Quality assessment failed",
                error=str(e)
            )
            return {
                "overall_score": 0.0,
                "rating": "error",
                "error": str(e)
            }

    def _extract_features(
        self,
        image: Image.Image,
        detailed: bool = True
    ) -> Dict[str, Any]:
        """Extract features from the image."""
        try:
            features = {
                "basic_features": self._extract_basic_features(image),
                "color_features": self._extract_color_features(image),
                "texture_features": self._extract_texture_features(image)
            }

            if detailed:
                features.update({
                    "spatial_features": self._extract_spatial_features(image),
                    "object_features": self._extract_object_features(image),
                    "style_features": self._extract_style_features(image)
                })

            return features

        except Exception as e:
            logger.error(
                "Feature extraction failed",
                error=str(e)
            )
            return {"error": str(e)}

    def _detect_objects(self, image: Image.Image) -> Dict[str, Any]:
        """Detect objects in the property image."""
        try:
            return self.object_detector.detect(image)

        except Exception as e:
            logger.error(
                "Object detection failed",
                error=str(e)
            )
            return {"error": str(e)}

    def _analyze_aesthetics(self, image: Image.Image) -> Dict[str, Any]:
        """Analyze aesthetic qualities of the image."""
        try:
            img_array = np.array(image)

            return {
                "rule_of_thirds": self._check_rule_of_thirds(img_array),
                "symmetry": self._calculate_symmetry(img_array),
                "color_harmony": self._calculate_color_harmony(img_array),
                "depth_perception": self._calculate_depth_perception(img_array),
                "lighting_quality": self._assess_lighting_quality(img_array),
                "clutter_score": self._calculate_clutter_score(img_array)
            }

        except Exception as e:
            logger.error(
                "Aesthetic analysis failed",
                error=str(e)
            )
            return {"error": str(e)}

    def _analyze_layout(self, image: Image.Image) -> Dict[str, Any]:
        """Analyze room layout and space utilization."""
        try:
            img_array = np.array(image)

            return {
                "space_utilization": self._calculate_space_utilization(img_array),
                "furniture_arrangement": self._analyze_furniture_arrangement(img_array),
                "open_concept": self._detect_open_concept(img_array),
                "natural_light": self._assess_natural_light(img_array),
                "room_proportions": self._analyze_room_proportions(img_array)
            }

        except Exception as e:
            logger.error(
                "Layout analysis failed",
                error=str(e)
            )
            return {"error": str(e)}

    def _process_batch(self, image_paths: List[Union[str, Path]]) -> List[Dict[str, Any]]:
        """Process a batch of images efficiently."""
        results = []

        try:
            # Load images
            images = [self._load_image(path) for path in image_paths]

            # Create batch tensor for neural networks
            batch_tensors = torch.stack([
                self.transform(img) for img in images
            ]).to(self.device)

            # Batch room classification
            with torch.no_grad():
                room_outputs = self.room_classifier(batch_tensors)
                room_probs = torch.softmax(room_outputs, dim=1)

            # Batch quality assessment
            with torch.no_grad():
                quality_outputs = self.quality_assessor(batch_tensors)

            # Process individual results
            for i, (image, image_path) in enumerate(zip(images, image_paths)):
                result = {
                    "image_path": str(image_path),
                    "timestamp": datetime.now().isoformat(),
                    "image_size": {"width": image.size[0], "height": image.size[1]},
                    "room_classification": {
                        "predicted_room": self.room_types[torch.argmax(room_probs[i]).item()],
                        "confidence": torch.max(room_probs[i]).item()
                    },
                    "quality_assessment": {
                        "nn_score": quality_outputs[i].item() * 100
                    }
                }

                results.append(result)

            return results

        except Exception as e:
            logger.error(
                "Batch processing failed",
                error=str(e)
            )
            # Fall back to individual processing
            for image_path in image_paths:
                try:
                    result = self.analyze_image(image_path)
                    results.append(result)
                except Exception as e:
                    results.append({
                        "image_path": str(image_path),
                        "error": str(e)
                    })

        return results

    # Helper methods for feature calculations
    def _calculate_brightness(self, img_array: np.ndarray) -> float:
        """Calculate average brightness (0-100)."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        return np.mean(gray) / 255 * 100

    def _calculate_contrast(self, img_array: np.ndarray) -> float:
        """Calculate contrast (0-100)."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        return np.std(gray) / 128 * 100

    def _calculate_sharpness(self, img_array: np.ndarray) -> float:
        """Calculate sharpness using Laplacian variance (0-100)."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        laplacian = cv2.Laplacian(gray, cv2.CV_64F)
        return min(laplacian.var() / 1000, 100)

    def _calculate_noise(self, img_array: np.ndarray) -> float:
        """Calculate noise level (0-100)."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        # High-pass filter to isolate noise
        kernel = np.array([[-1, -1, -1], [-1, 9, -1], [-1, -1, -1]])
        filtered = cv2.filter2D(gray, -1, kernel)
        return np.std(filtered) / 128 * 100

    def _calculate_resolution(self, img_array: np.ndarray) -> float:
        """Calculate effective resolution score (0-100)."""
        height, width = img_array.shape[:2]
        # Higher resolution is better, up to a point
        pixels = height * width
        # Normalize to 0-100 scale
        return min(pixels / (1920 * 1080) * 100, 100)

    def _calculate_composition_score(self, img_array: np.ndarray) -> float:
        """Calculate composition score based on edge distribution."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        edges = cv2.Canny(gray, 50, 150)

        # Divide image into thirds
        h, w = edges.shape
        third_h, third_w = h // 3, w // 3

        # Check for edges in rule-of-thirds lines
        horizontal_thirds = edges[third_h, :] + edges[2 * third_h, :]
        vertical_thirds = edges[:, third_w] + edges[:, 2 * third_w]

        # Score based on edge density in thirds
        horizontal_score = np.mean(horizontal_thirds) / 255 * 100
        vertical_score = np.mean(vertical_thirds) / 255 * 100

        return (horizontal_score + vertical_score) / 2

    def _calculate_color_balance(self, img_array: np.ndarray) -> float:
        """Calculate color balance score (0-100)."""
        mean_r = np.mean(img_array[:, :, 0])
        mean_g = np.mean(img_array[:, :, 1])
        mean_b = np.mean(img_array[:, :, 2])

        # Calculate deviation from gray
        gray_mean = (mean_r + mean_g + mean_b) / 3
        deviation = np.sqrt(
            (mean_r - gray_mean) ** 2 +
            (mean_g - gray_mean) ** 2 +
            (mean_b - gray_mean) ** 2
        )

        # Convert to score (lower deviation is better)
        return max(100 - deviation, 0)

    def _extract_basic_features(self, image: Image.Image) -> Dict[str, Any]:
        """Extract basic image features."""
        img_array = np.array(image)
        h, w = img_array.shape[:2]

        return {
            "aspect_ratio": w / h,
            "area": w * h,
            "format": image.format,
            "mode": image.mode
        }

    def _extract_color_features(self, image: Image.Image) -> Dict[str, Any]:
        """Extract color-based features."""
        img_array = np.array(image)
        return {
            "dominant_colors": self._get_dominant_colors(img_array),
            "color_temperature": self._estimate_color_temperature(img_array),
            "saturation": self._calculate_saturation(img_array),
            "color_variance": self._calculate_color_variance(img_array)
        }

    def _get_dominant_colors(self, img_array: np.ndarray, k: int = 5) -> List[List[int]]:
        """Get k dominant colors using k-means clustering."""
        from sklearn.cluster import KMeans

        # Reshape image to list of pixels
        pixels = img_array.reshape(-1, 3)

        # Sample pixels for efficiency
        if len(pixels) > 10000:
            indices = np.random.choice(len(pixels), 10000, replace=False)
            pixels = pixels[indices]

        # Perform k-means clustering
        kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
        kmeans.fit(pixels)

        # Get cluster centers (dominant colors)
        colors = kmeans.cluster_centers_.astype(int).tolist()
        return colors

    def _estimate_color_temperature(self, img_array: np.ndarray) -> float:
        """Estimate color temperature in Kelvin."""
        # Calculate average RGB values
        avg_r = np.mean(img_array[:, :, 0])
        avg_g = np.mean(img_array[:, :, 1])
        avg_b = np.mean(img_array[:, :, 2])

        # Simple temperature estimation
        # Red > Blue: warm (lower Kelvin)
        # Blue > Red: cool (higher Kelvin)
        if avg_r > avg_b:
            # Warm light (2700K - 3500K)
            ratio = avg_r / (avg_b + 1)
            temp = 3000 + (1 - min(ratio, 2)) * 500
        else:
            # Cool light (4000K - 6500K)
            ratio = avg_b / (avg_r + 1)
            temp = 4000 + min(ratio, 2.5) * 1000

        return round(temp, 0)

    def _calculate_saturation(self, img_array: np.ndarray) -> float:
        """Calculate average color saturation."""
        # Convert to HSV
        hsv = cv2.cvtColor(img_array, cv2.COLOR_RGB2HSV)
        return np.mean(hsv[:, :, 1]) / 255 * 100

    def _calculate_color_variance(self, img_array: np.ndarray) -> float:
        """Calculate color variance."""
        return np.mean(np.var(img_array, axis=(0, 1))) / 255 * 100

    def _extract_texture_features(self, image: Image.Image) -> Dict[str, Any]:
        """Extract texture features using GLCM."""
        img_array = np.array(image)
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

        # Calculate GLCM properties
        from skimage.feature import greycomatrix, greycoprops

        glcm = greycomatrix(
            gray,
            distances=[1, 2, 3],
            angles=[0, np.pi/4, np.pi/2, 3*np.pi/4],
            symmetric=True,
            normed=True
        )

        return {
            "contrast": np.mean(greycoprops(glcm, 'contrast')),
            "dissimilarity": np.mean(greycoprops(glcm, 'dissimilarity')),
            "homogeneity": np.mean(greycoprops(glcm, 'homogeneity')),
            "energy": np.mean(greycoprops(glcm, 'energy')),
            "correlation": np.mean(greycoprops(glcm, 'correlation'))
        }

    def _extract_spatial_features(self, image: Image.Image) -> Dict[str, Any]:
        """Extract spatial features."""
        img_array = np.array(image)
        return {
            "edge_density": self._calculate_edge_density(img_array),
            "line_orientation": self._analyze_line_orientation(img_array),
            "depth_cues": self._detect_depth_cues(img_array)
        }

    def _calculate_edge_density(self, img_array: np.ndarray) -> float:
        """Calculate edge density."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        edges = cv2.Canny(gray, 50, 150)
        return np.sum(edges > 0) / edges.size * 100

    def _analyze_line_orientation(self, img_array: np.ndarray) -> Dict[str, float]:
        """Analyze line orientations using Hough transform."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        lines = cv2.HoughLinesP(gray, 1, np.pi/180, threshold=50)

        if lines is None:
            return {"horizontal": 0, "vertical": 0, "diagonal": 0}

        angles = []
        for line in lines:
            x1, y1, x2, y2 = line[0]
            angle = np.arctan2(y2 - y1, x2 - x1) * 180 / np.pi
            angles.append(angle)

        # Categorize angles
        horizontal = sum(1 for a in angles if abs(a) < 30 or abs(a - 180) < 30)
        vertical = sum(1 for a in angles if abs(a - 90) < 30 or abs(a + 90) < 30)
        diagonal = len(angles) - horizontal - vertical

        total = len(angles) or 1
        return {
            "horizontal": horizontal / total * 100,
            "vertical": vertical / total * 100,
            "diagonal": diagonal / total * 100
        }

    def _detect_depth_cues(self, img_array: np.ndarray) -> Dict[str, float]:
        """Detect depth cues in the image."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

        # Linear perspective (vanishing point detection)
        edges = cv2.Canny(gray, 50, 150)
        lines = cv2.HoughLines(edges, 1, np.pi/180, threshold=100)

        perspective_score = 0
        if lines is not None:
            # Look for converging lines (perspective)
            angles = [line[0][1] for line in lines]
            unique_angles = len(set(int(a * 180 / np.pi) % 180 for a in angles))
            perspective_score = (180 - unique_angles) / 180 * 100

        return {
            "linear_perspective": perspective_score,
            "size_gradient": self._calculate_size_gradient(gray),
            "atmospheric_perspective": self._calculate_atmospheric_perspective(img_array)
        }

    def _calculate_size_gradient(self, gray: np.ndarray) -> float:
        """Calculate size gradient for depth perception."""
        # Divide image into horizontal strips
        h, w = gray.shape
        strip_size = h // 10

        means = []
        for i in range(0, h, strip_size):
            strip = gray[i:i + strip_size, :]
            means.append(np.mean(strip))

        # Calculate gradient (should increase from top to bottom for perspective)
        if len(means) < 2:
            return 0

        gradient = np.gradient(means)
        return np.mean(np.abs(gradient)) / 255 * 100

    def _calculate_atmospheric_perspective(self, img_array: np.ndarray) -> float:
        """Calculate atmospheric perspective (haze/distance effect)."""
        # Measure contrast reduction with distance
        h, w = img_array.shape[:2]
        top_half = img_array[:h//2, :]
        bottom_half = img_array[h//2:, :]

        top_contrast = np.std(cv2.cvtColor(top_half, cv2.COLOR_RGB2GRAY))
        bottom_contrast = np.std(cv2.cvtColor(bottom_half, cv2.COLOR_RGB2GRAY))

        # Atmospheric perspective causes distant objects (top) to have less contrast
        if bottom_contrast > 0:
            perspective = (1 - top_contrast / bottom_contrast) * 100
        else:
            perspective = 0

        return max(0, min(perspective, 100))

    def _extract_object_features(self, image: Image.Image) -> Dict[str, Any]:
        """Extract object-related features."""
        return {
            "foreground_objects": self._count_foreground_objects(image),
            "occlusion_level": self._measure_occlusion(image),
            "object_clutter": self._measure_object_clutter(image)
        }

    def _count_foreground_objects(self, image: Image.Image) -> int:
        """Count distinct foreground objects."""
        img_array = np.array(image)
        # Use contour detection to find objects
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)

        contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        # Filter small contours
        min_area = (img_array.shape[0] * img_array.shape[1]) // 1000
        objects = [c for c in contours if cv2.contourArea(c) > min_area]

        return len(objects)

    def _measure_occlusion(self, image: Image.Image) -> float:
        """Measure occlusion level (0-100)."""
        # Simple heuristic: high edge density suggests occlusion
        img_array = np.array(image)
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        edges = cv2.Canny(gray, 50, 150)

        edge_density = np.sum(edges > 0) / edges.size

        # Normalize to 0-100 scale
        return min(edge_density * 10, 100)

    def _measure_object_clutter(self, image: Image.Image) -> float:
        """Measure clutter level in the image."""
        img_array = np.array(image)
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

        # Use local binary patterns to measure texture complexity
        from skimage.feature import local_binary_pattern

        lbp = local_binary_pattern(gray, P=8, R=1, method="uniform")
        hist, _ = np.histogram(lbp.ravel(), bins=10)

        # Higher entropy suggests more clutter
        hist = hist / hist.sum()
        entropy = -np.sum(hist * np.log2(hist + 1e-10))

        return min(entropy * 20, 100)

    def _extract_style_features(self, image: Image.Image) -> Dict[str, Any]:
        """Extract architectural style features."""
        return {
            "architectural_style": self._classify_architectural_style(image),
            "decor_elements": self._detect_decorative_elements(image),
            "modern_vs_traditional": self._assess_modern_vs_traditional(image)
        }

    def _classify_architectural_style(self, image: Image.Image) -> str:
        """Classify architectural style."""
        # This would typically use a trained classifier
        # For now, return a placeholder
        return "contemporary"

    def _detect_decorative_elements(self, image: Image.Image) -> List[str]:
        """Detect decorative elements in the image."""
        elements = []

        # Check for common decorative patterns
        img_array = np.array(image)
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

        # Detect patterns (simplified)
        edges = cv2.Canny(gray, 50, 150)
        lines = cv2.HoughLinesP(edges, 1, np.pi/180, threshold=50)

        if lines is not None and len(lines) > 10:
            elements.append("geometric_patterns")

        # Check for curves (arches, circles)
        circles = cv2.HoughCircles(gray, cv2.HOUGH_GRADIENT, 1, 20, 100, 100)
        if circles is not None and len(circles[0]) > 0:
            elements.append("curved_elements")

        return elements

    def _assess_modern_vs_traditional(self, image: Image.Image) -> float:
        """Assess if the style is more modern (closer to 100) or traditional (closer to 0)."""
        img_array = np.array(image)

        # Modern features: clean lines, minimal ornamentation
        # Traditional features: ornate details, patterns

        # Simple heuristic based on edge characteristics
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        edges = cv2.Canny(gray, 50, 150)

        # Modern: fewer, longer edges (clean lines)
        # Traditional: more, shorter edges (details)
        lines = cv2.HoughLinesP(edges, 1, np.pi/180, threshold=50)

        if lines is not None:
            avg_length = np.mean([abs(line[0][2] - line[0][0]) + abs(line[0][3] - line[0][1])
                               for line in lines])
            # Longer average edges suggest modern style
            modern_score = min(avg_length / 100, 100)
        else:
            modern_score = 50

        return modern_score

    def _check_rule_of_thirds(self, img_array: np.ndarray) -> float:
        """Check if image follows rule of thirds (0-100)."""
        h, w = img_array.shape[:2]
        third_h, third_w = h // 3, w // 3

        # Calculate edges at third lines
        edges = cv2.Canny(cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY), 50, 150)

        # Check for elements at intersection points
        intersections = [
            (third_w, third_h),
            (2 * third_w, third_h),
            (third_w, 2 * third_h),
            (2 * third_w, 2 * third_w)
        ]

        score = 0
        for x, y in intersections:
            # Check for edges around intersection
            roi = edges[max(0, y-20):min(h, y+20), max(0, x-20):min(w, x+20)]
            if np.sum(roi) > 1000:
                score += 25

        return score

    def _calculate_symmetry(self, img_array: np.ndarray) -> float:
        """Calculate symmetry score (0-100)."""
        h, w = img_array.shape[:2]
        left_half = img_array[:, :w//2]
        right_half = img_array[:, w//2:]

        # Mirror right half to compare with left
        right_mirrored = cv2.flip(right_half, 1)

        # Calculate similarity
        diff = cv2.absdiff(left_half, right_mirrored)
        similarity = 100 - (np.mean(diff) / 255 * 100)

        return similarity

    def _calculate_color_harmony(self, img_array: np.ndarray) -> float:
        """Calculate color harmony score."""
        # Convert to HSV
        hsv = cv2.cvtColor(img_array, cv2.COLOR_RGB2HSV)

        # Check for complementary colors
        hue_hist = cv2.calcHist([hsv], [0], None, [180], [0, 180])
        hue_hist = hue_hist / hue_hist.sum()

        # Find dominant hues
        dominant_hues = np.argsort(hue_hist)[-3:]

        # Check for harmony (opposite hues on color wheel)
        harmony_score = 0
        for i in range(len(dominant_hues)):
            for j in range(i + 1, len(dominant_hues)):
                hue_diff = abs(dominant_hues[i] - dominant_hues[j])
                if 85 <= hue_diff <= 95:  # Complementary colors
                    harmony_score += 50
                elif 165 <= hue_diff <= 180 or 0 <= hue_diff <= 15:  # Same/analogous
                    harmony_score += 30

        return min(harmony_score, 100)

    def _calculate_depth_perception(self, img_array: np.ndarray) -> float:
        """Calculate depth perception score."""
        # Use multiple depth cues
        perspective = self._detect_depth_cues(img_array)
        return perspective["linear_perspective"]

    def _assess_lighting_quality(self, img_array: np.ndarray) -> Dict[str, Any]:
        """Assess lighting quality."""
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

        # Calculate histograms for different regions
        h, w = gray.shape
        center = gray[h//3:2*h//3, w//3:2*w//3]
        corners = [
            gray[:h//3, :w//3],  # Top-left
            gray[:h//3, 2*w//3:],  # Top-right
            gray[2*h//3:, :w//3],  # Bottom-left
            gray[2*h//3:, 2*w//3:]  # Bottom-right
        ]

        center_brightness = np.mean(center)
        corner_brightness = [np.mean(corner) for corner in corners]
        avg_corner_brightness = np.mean(corner_brightness)

        return {
            "lighting_type": "natural" if center_brightness > avg_corner_brightness * 1.2 else "artificial",
            "center_brightness": center_brightness,
            "corner_brightness": avg_corner_brightness,
            "uniformity": 100 - abs(center_brightness - avg_corner_brightness) / 255 * 100,
            "shadows_present": np.std(corner_brightness) / 255 * 100
        }

    def _calculate_clutter_score(self, img_array: np.ndarray) -> float:
        """Calculate clutter score (0-100, higher is more cluttered)."""
        # Use edge density as proxy for clutter
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        edges = cv2.Canny(gray, 30, 100)

        # High edge density suggests clutter
        edge_density = np.sum(edges > 0) / edges.size

        # Also check for small objects
        _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
        contours, _ = cv2.findContours(thresh, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)

        # Count small objects
        min_area = (img_array.shape[0] * img_array.shape[1]) // 500
        small_objects = sum(1 for c in contours if cv2.contourArea(c) < min_area)

        clutter_score = (edge_density * 50 + min(small_objects * 2, 50))

        return min(clutter_score, 100)

    def _calculate_space_utilization(self, img_array: np.ndarray) -> Dict[str, float]:
        """Calculate space utilization metrics."""
        h, w = img_array.shape[:2]

        # Floor space estimation
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        # Find floor area (largest contiguous area at bottom)
        floor_region = thresh[int(h * 0.7):, :]
        floor_area = np.sum(floor_region == 255) / floor_region.size * 100

        # Furniture space
        furniture_area = np.sum(thresh == 0) / thresh.size * 100 - floor_area

        return {
            "floor_utilization": floor_area,
            "furniture_coverage": furniture_area,
            "open_space": max(0, 100 - floor_area - furniture_area),
            "vertical_space_usage": self._calculate_vertical_space_usage(img_array)
        }

    def _calculate_vertical_space_usage(self, img_array: np.ndarray) -> float:
        """Calculate vertical space utilization."""
        h = img_array.shape[0]
        thirds = h // 3

        # Analyze each third
        bottom_third = img_array[2 * thirds:, :]
        middle_third = img_array[thirds:2 * thirds, :]
        top_third = img_array[:thirds, :]

        # Calculate activity in each third
        def get_activity(region):
            gray = cv2.cvtColor(region, cv2.COLOR_RGB2GRAY)
            return np.std(gray)

        bottom_activity = get_activity(bottom_third)
        middle_activity = get_activity(middle_third)
        top_activity = get_activity(top_third)

        # Ideal utilization has activity throughout
        utilization = (bottom_activity + middle_activity + top_activity) / 3

        return min(utilization / 50 * 100, 100)

    def _analyze_furniture_arrangement(self, img_array: np.ndarray) -> Dict[str, Any]:
        """Analyze furniture arrangement."""
        # This would require sophisticated object detection
        # For now, return placeholder
        return {
            "arrangement_style": "balanced",
            "flow_score": 75.0,
            "furniture_count": 5
        }

    def _detect_open_concept(self, img_array: np.ndarray) -> bool:
        """Detect if image shows an open concept space."""
        # Look for lack of walls between areas
        h, w = img_array.shape[:2]
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

        # Check for vertical lines that would indicate walls
        edges = cv2.Canny(gray, 50, 150)
        lines = cv2.HoughLinesP(edges, 1, np.pi/180, threshold=50, minLineLength=w//4)

        if lines is not None:
            vertical_lines = [
                line for line in lines
                if abs(line[0][3] - line[0][1]) > abs(line[0][2] - line[0][0])
            ]
            # Fewer vertical lines might indicate open concept
            return len(vertical_lines) < 5

        return True  # Default to open concept if no walls detected

    def _assess_natural_light(self, img_array: np.ndarray) -> Dict[str, Any]:
        """Assess natural lighting."""
        # Look for bright areas (windows)
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

        # Threshold to find bright areas
        _, bright_mask = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY)

        # Check distribution of bright areas
        contours, _ = cv2.findContours(bright_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        window_areas = [cv2.contourArea(c) for c in contours if cv2.contourArea(c) > 100]

        return {
            "window_count": len(window_areas),
            "natural_light_score": min(sum(window_areas) / (gray.size * 0.1) * 100, 100),
            "light_distribution": "balanced" if len(window_areas) > 2 else "limited"
        }

    def _analyze_room_proportions(self, img_array: np.ndarray) -> Dict[str, Any]:
        """Analyze room proportions."""
        h, w = img_array.shape[:2]
        aspect_ratio = w / h

        # Golden ratio ~ 1.618
        golden_deviation = abs(aspect_ratio - 1.618) / 1.618 * 100

        return {
            "aspect_ratio": round(aspect_ratio, 2),
            "golden_ratio_score": max(100 - golden_deviation * 2, 0),
            "squareness": min(aspect_ratio, 1/aspect_ratio) * 100,
            "room_type": self._estimate_room_type_by_proportions(aspect_ratio)
        }

    def _estimate_room_type_by_proportions(self, aspect_ratio: float) -> str:
        """Estimate room type based on aspect ratio."""
        if aspect_ratio > 2:
            return "hallway_or_corridor"
        elif aspect_ratio > 1.5:
            return "living_room_or_kitchen"
        elif 0.8 < aspect_ratio < 1.2:
            return "bedroom_or_office"
        else:
            return "bathroom_or_closet"


class RoomClassifier(nn.Module):
    """CNN for room type classification."""

    def __init__(self, num_classes: int = 12):
        super().__init__()
        self.features = nn.Sequential(
            # First block
            nn.Conv2d(3, 64, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(inplace=True),
            nn.Conv2d(64, 64, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),

            # Second block
            nn.Conv2d(64, 128, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(inplace=True),
            nn.Conv2d(128, 128, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),

            # Third block
            nn.Conv2d(128, 256, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(256),
            nn.ReLU(inplace=True),
            nn.Conv2d(256, 256, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(256),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),

            # Fourth block
            nn.Conv2d(256, 512, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(512),
            nn.ReLU(inplace=True),
            nn.Conv2d(512, 512, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(512),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2)
        )

        # Adaptive pooling to handle variable input sizes
        self.adaptive_pool = nn.AdaptiveAvgPool2d((1, 1))

        # Classifier
        self.classifier = nn.Sequential(
            nn.Dropout(0.5),
            nn.Linear(512, 256),
            nn.ReLU(inplace=True),
            nn.Dropout(0.5),
            nn.Linear(256, 128),
            nn.ReLU(inplace=True),
            nn.Linear(128, num_classes)
        )

    def forward(self, x):
        x = self.features(x)
        x = self.adaptive_pool(x)
        x = x.view(x.size(0), -1)
        x = self.classifier(x)
        return x


class ImageQualityAssessor(nn.Module):
    """Neural network for image quality assessment."""

    def __init__(self):
        super().__init__()
        self.features = nn.Sequential(
            # Feature extraction layers
            nn.Conv2d(3, 32, kernel_size=3, stride=1, padding=1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),

            nn.Conv2d(32, 64, kernel_size=3, stride=1, padding=1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),

            nn.Conv2d(64, 128, kernel_size=3, stride=1, padding=1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),

            nn.Conv2d(128, 256, kernel_size=3, stride=1, padding=1),
            nn.ReLU(inplace=True),
            nn.AdaptiveAvgPool2d((1, 1))
        )

        # Quality predictor
        self.predictor = nn.Sequential(
            nn.Dropout(0.5),
            nn.Linear(256, 128),
            nn.ReLU(inplace=True),
            nn.Dropout(0.5),
            nn.Linear(128, 64),
            nn.ReLU(inplace=True),
            nn.Linear(64, 1),
            nn.Sigmoid()
        )

    def forward(self, x):
        x = self.features(x)
        x = x.view(x.size(0), -1)
        x = self.predictor(x)
        return x


class PropertyObjectDetector:
    """Object detector for property-specific objects."""

    def __init__(self):
        self.model = None
        self.class_names = []

    def load_model(self, model_path):
        """Load YOLO model."""
        try:
            import yolov5
            self.model = yolov5.load(str(model_path), device='cuda' if torch.cuda.is_available() else 'cpu')
            self.class_names = self.model.names
        except Exception as e:
            logger.warning(
                "Could not load YOLO model, using fallback",
                error=str(e)
            )
            self.model = None

    def detect(self, image: Image.Image) -> Dict[str, Any]:
        """Detect objects in the image."""
        if self.model is None:
            return self._fallback_detection(image)

        results = self.model(image)
        detections = []

        for *box, conf, cls in results.xyxy[0].cpu().numpy():
            if conf > 0.5:  # Confidence threshold
                class_name = self.class_names[int(cls)]
                detections.append({
                    "class": class_name,
                    "confidence": float(conf),
                    "bbox": box.tolist(),
                    "center": [
                        float((box[0] + box[2]) / 2),
                        float((box[1] + box[3]) / 2)
                    ]
                })

        return {
            "detections": detections,
            "count": len(detections),
            "categories": list(set(d["class"] for d in detections))
        }

    def _fallback_detection(self, image: Image.Image) -> Dict[str, Any]:
        """Fallback object detection using basic CV techniques."""
        img_array = np.array(image)

        # Basic object detection using contours
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
        _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        detections = []
        for contour in contours:
            area = cv2.contourArea(contour)
            if area > 1000:  # Minimum area threshold
                x, y, w, h = cv2.boundingRect(contour)
                detections.append({
                    "class": "object",
                    "confidence": 0.5,
                    "bbox": [x, y, x + w, y + h],
                    "center": [x + w // 2, y + h // 2]
                })

        return {
            "detections": detections,
            "count": len(detections),
            "categories": ["object"]
        }