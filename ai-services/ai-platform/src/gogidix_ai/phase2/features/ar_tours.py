"""
AR/VR 3D Property Tours Module

Implements augmented and virtual reality property tours with:
- 3D model generation from images
- VR headset support
- Mobile AR viewing
- Interactive hotspots
- Virtual staging
"""

import asyncio
import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
import uuid

import cv2
import numpy as np
from PIL import Image, ImageEnhance
import trimesh
from pyntcloud import PyntCloud
import plotly.graph_objects as go
import plotly.express as px
from fastapi import HTTPException, status
from pydantic import BaseModel, Field

from ..core.config import get_settings
from ..core.logging import get_logger
from ..core.exceptions import ProcessingError

logger = get_logger(__name__)
settings = get_settings()


class ARPointOfInterest(BaseModel):
    """AR point of interest in property tour."""
    id: str = Field(..., description="Unique identifier")
    type: str = Field(..., description="Type of POI (e.g., 'appliance', 'feature', 'info')")
    position: List[float] = Field(..., description="3D position [x, y, z]")
    title: str = Field(..., description="Title of the POI")
    description: str = Field(..., description="Detailed description")
    media_url: Optional[str] = Field(None, description="URL to image or video")
    action: Optional[Dict[str, Any]] = Field(None, description="Interactive action")


class ARTourConfig(BaseModel):
    """Configuration for AR/VR property tour."""
    property_id: str = Field(..., description="Property ID")
    room_name: str = Field(..., description="Room name")
    tour_type: str = Field(default="ar", description="Tour type: ar, vr, or web")
    resolution: Tuple[int, int] = Field(default=(1920, 1080))
    include_measurements: bool = Field(default=True)
    include_virtual_staging: bool = Field(default=False)
    style_preference: str = Field(default="modern")  # modern, classic, minimalist


class ARTourModel(BaseModel):
    """Complete AR/VR tour model."""
    tour_id: str = Field(..., description="Unique tour identifier")
    property_id: str = Field(..., description="Associated property ID")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    model_url: str = Field(..., description="URL to 3D model")
    thumbnail_url: str = Field(..., description="URL to thumbnail")
    vr_url: Optional[str] = Field(None, description="URL to VR experience")
    ar_url: Optional[str] = Field(None, description="URL to AR experience")
    web_url: Optional[str] = Field(None, description="URL to web viewer")
    pois: List[ARPointOfInterest] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class ARVirtualStaging:
    """Virtual staging for empty properties."""

    def __init__(self):
        # Load furniture models database
        self.furniture_models = self._load_furniture_models()

    def _load_furniture_models(self) -> Dict[str, Any]:
        """Load virtual furniture models by style."""
        return {
            "modern": {
                "sofa": "models/furniture/modern/sofa.obj",
                "table": "models/furniture/modern/table.obj",
                "bed": "models/furniture/modern/bed.obj",
                "chair": "models/furniture/modern/chair.obj"
            },
            "classic": {
                "sofa": "models/furniture/classic/sofa.obj",
                "table": "models/furniture/classic/table.obj",
                "bed": "models/furniture/classic/bed.obj",
                "chair": "models/furniture/classic/chair.obj"
            },
            "minimalist": {
                "sofa": "models/furniture/minimalist/sofa.obj",
                "table": "models/furniture/minimalist/table.obj",
                "bed": "models/furniture/minimalist/bed.obj",
                "chair": "models/furniture/minimalist/chair.obj"
            }
        }

    async def stage_room(self, room_mesh: trimesh.Trimesh,
                        room_type: str, style: str) -> trimesh.Trimesh:
        """Apply virtual staging to a room mesh."""
        logger.info(f"Virtual staging {room_type} with {style} style")

        # Get furniture for room type and style
        furniture_map = {
            "living_room": ["sofa", "table", "chair"],
            "bedroom": ["bed", "chair", "table"],
            "dining_room": ["table", "chair"],
            "office": ["table", "chair"]
        }

        furniture_items = furniture_map.get(room_type, [])
        styled_room = room_mesh.copy()

        # Place furniture in room
        for item in furniture_items:
            furniture_path = self.furniture_models[style].get(item)
            if furniture_path and Path(furniture_path).exists():
                furniture_mesh = trimesh.load(furniture_path)

                # Calculate placement position
                position = self._calculate_furniture_placement(
                    styled_room, item, room_type
                )

                # Place furniture
                furniture_mesh.apply_translation(position)
                styled_room = styled_room.union(furniture_mesh)

        return styled_room

    def _calculate_furniture_placement(self, room_mesh: trimesh.Trimesh,
                                     item: str, room_type: str) -> np.ndarray:
        """Calculate optimal placement for furniture item."""
        # Get room bounds
        bounds = room_mesh.bounds
        room_center = room_mesh.centroid

        # Simplified placement logic
        if item == "sofa":
            # Place sofa against longest wall
            return np.array([room_center[0], bounds[0][1] + 1.0, 0])
        elif item == "table":
            # Place table in center
            return np.array([room_center[0], room_center[1], 0])
        elif item == "bed":
            # Place bed against wall
            return np.array([room_center[0], bounds[0][1] + 0.5, 0])
        elif item == "chair":
            # Place chair near table
            return np.array([room_center[0] + 1.5, room_center[1], 0])

        return room_center


class ThreeDModelGenerator:
    """Generates 3D models from 2D images."""

    def __init__(self):
        self.depth_estimator = self._init_depth_estimator()
        self.staging = ARVirtualStaging()

    def _init_depth_estimator(self):
        """Initialize depth estimation model."""
        # In production, use MiDaS or similar depth estimation
        return None

    async def generate_mesh_from_images(self,
                                      images: List[np.ndarray],
                                      room_type: str) -> trimesh.Trimesh:
        """Generate 3D mesh from room images."""
        logger.info(f"Generating 3D mesh for {room_type} from {len(images)} images")

        # Structure from Motion (SfM) pipeline
        points_3d, colors = await self._sfm_reconstruction(images)

        # Create point cloud
        cloud = PyntCloud.from_array(points_3d)

        # Mesh reconstruction
        mesh = cloud.to_mesh()

        # Clean and optimize mesh
        mesh = self._clean_mesh(mesh)

        return mesh

    async def _sfm_reconstruction(self, images: List[np.ndarray]) -> Tuple[np.ndarray, np.ndarray]:
        """Perform Structure from Motion reconstruction."""
        # Extract features
        keypoints_list = []
        descriptors_list = []

        for img in images:
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            sift = cv2.SIFT_create()
            kp, desc = sift.detectAndCompute(gray, None)
            keypoints_list.append(kp)
            descriptors_list.append(desc)

        # Match features between images
        matches = []
        for i in range(len(images) - 1):
            bf = cv2.BFMatcher()
            matches.append(bf.match(descriptors_list[i], descriptors_list[i + 1]))

        # Estimate camera poses (simplified)
        poses = self._estimate_camera_poses(images, matches)

        # Triangulate 3D points
        points_3d = self._triangulate_points(images, keypoints_list, matches, poses)

        # Extract colors
        colors = self._extract_colors(images, points_3d, poses)

        return points_3d, colors

    def _clean_mesh(self, mesh: trimesh.Trimesh) -> trimesh.Trimesh:
        """Clean and optimize 3D mesh."""
        # Remove duplicate vertices
        mesh.remove_duplicate_faces()
        mesh.remove_unreferenced_vertices()

        # Fill holes
        mesh.fill_holes()

        # Smooth mesh
        mesh = mesh.smoothed()

        return mesh


class ARVirtualTourService:
    """Main service for AR/VR property tours."""

    def __init__(self):
        self.model_generator = ThreeDModelGenerator()
        self.tours_storage = Path(settings.ASSETS_DIR) / "tours"
        self.tours_storage.mkdir(parents=True, exist_ok=True)

    async def create_tour(self, config: ARTourConfig,
                         images: List[bytes]) -> ARTourModel:
        """Create AR/VR tour from property images."""
        logger.info(f"Creating AR/VR tour for property {config.property_id}")

        try:
            # Generate unique tour ID
            tour_id = str(uuid.uuid4())

            # Convert images to numpy arrays
            image_arrays = []
            for img_bytes in images:
                img_array = np.array(Image.open(io.BytesIO(img_bytes)))
                image_arrays.append(img_array)

            # Generate 3D model
            room_mesh = await self.model_generator.generate_mesh_from_images(
                image_arrays, config.room_name
            )

            # Apply virtual staging if requested
            if config.include_virtual_staging:
                room_mesh = await self.model_generator.staging.stage_room(
                    room_mesh, config.room_name, config.style_preference
                )

            # Add measurement annotations
            if config.include_measurements:
                room_mesh = self._add_measurements(room_mesh)

            # Save 3D model
            model_path = self.tours_storage / f"{tour_id}.obj"
            room_mesh.export(str(model_path))

            # Generate renderings
            thumbnail_url = await self._generate_thumbnail(room_mesh, tour_id)

            # Create different format exports
            vr_url = await self._export_vr_format(room_mesh, tour_id)
            ar_url = await self._export_ar_format(room_mesh, tour_id)
            web_url = await self._export_web_format(room_mesh, tour_id)

            # Detect points of interest
            pois = await self._detect_pois(room_mesh, image_arrays)

            # Create tour model
            tour = ARTourModel(
                tour_id=tour_id,
                property_id=config.property_id,
                model_url=f"/tours/{tour_id}.obj",
                thumbnail_url=thumbnail_url,
                vr_url=vr_url,
                ar_url=ar_url,
                web_url=web_url,
                pois=pois,
                metadata={
                    "room_name": config.room_name,
                    "vertices": len(room_mesh.vertices),
                    "faces": len(room_mesh.faces),
                    "virtual_staged": config.include_virtual_staging,
                    "style": config.style_preference
                }
            )

            # Save tour metadata
            await self._save_tour_metadata(tour)

            return tour

        except Exception as e:
            logger.error(f"Failed to create AR/VR tour: {e}")
            raise ProcessingError(f"Tour creation failed: {str(e)}")

    async def _generate_thumbnail(self, mesh: trimesh.Trimesh,
                                 tour_id: str) -> str:
        """Generate thumbnail image of 3D model."""
        # Render using trimesh
        scene = mesh.scene()
        png = scene.save_image()

        # Save thumbnail
        thumbnail_path = self.tours_storage / f"{tour_id}_thumb.png"
        with open(thumbnail_path, "wb") as f:
            f.write(png)

        return f"/tours/{tour_id}_thumb.png"

    async def _export_vr_format(self, mesh: trimesh.Trimesh,
                              tour_id: str) -> str:
        """Export model for VR viewing (GLB format)."""
        vr_path = self.tours_storage / f"{tour_id}.glb"
        mesh.export(str(vr_path))
        return f"/tours/{tour_id}.glb"

    async def _export_ar_format(self, mesh: trimesh.Trimesh,
                              tour_id: str) -> str:
        """Export model for AR viewing (USDZ format)."""
        # Convert to USDZ for ARKit/ARCore
        ar_path = self.tours_storage / f"{tour_id}.usdz"
        mesh.export(str(ar_path))
        return f"/tours/{tour_id}.usdz"

    async def _export_web_format(self, mesh: trimesh.Trimesh,
                               tour_id: str) -> str:
        """Export model for web viewing (Three.js format)."""
        # Export to JSON for Three.js
        web_data = {
            "vertices": mesh.vertices.tolist(),
            "faces": mesh.faces.tolist(),
            "normals": mesh.vertex_normals.tolist()
        }

        web_path = self.tours_storage / f"{tour_id}.json"
        with open(web_path, "w") as f:
            json.dump(web_data, f)

        return f"/tours/{tour_id}.json"

    async def _detect_pois(self, mesh: trimesh.Trimesh,
                          images: List[np.ndarray]) -> List[ARPointOfInterest]:
        """Detect points of interest in the room."""
        pois = []

        # Detect windows
        windows = await self._detect_windows(images)
        for i, window in enumerate(windows):
            pois.append(ARPointOfInterest(
                id=f"window_{i}",
                type="feature",
                position=window["position"],
                title="Window",
                description=f"Natural lighting with {window['quality']} views",
                media_url=window.get("image_url")
            ))

        # Detect appliances
        appliances = await self._detect_appliances(images)
        for i, appliance in enumerate(appliances):
            pois.append(ARPointOfInterest(
                id=f"appliance_{i}",
                type="appliance",
                position=appliance["position"],
                title=appliance["name"],
                description=f"{appliance['brand']} {appliance['model']}",
                action={"type": "details", "data": appliance}
            ))

        # Add room measurements
        dimensions = self._get_room_dimensions(mesh)
        pois.append(ARPointOfInterest(
            id="measurements",
            type="info",
            position=[0, 0, 2.5],  # Ceiling height
            title="Room Dimensions",
            description=f"Size: {dimensions['length']}x{dimensions['width']}m, "
                       f"Height: {dimensions['height']}m, Area: {dimensions['area']}m²",
            action={"type": "measure"}
        ))

        return pois

    def _get_room_dimensions(self, mesh: trimesh.Trimesh) -> Dict[str, float]:
        """Calculate room dimensions from mesh."""
        bounds = mesh.bounds
        length = bounds[1][0] - bounds[0][0]
        width = bounds[1][1] - bounds[0][1]
        height = bounds[1][2] - bounds[0][2]
        area = length * width

        return {
            "length": round(length, 2),
            "width": round(width, 2),
            "height": round(height, 2),
            "area": round(area, 2)
        }


# AR/VR Experience Classes
class VRExperience:
    """VR experience for property tours."""

    def __init__(self, tour: ARTourModel):
        self.tour = tour
        self.viewer_position = [0, 0, 1.7]  # Average eye height

    def get_vr_scene_data(self) -> Dict[str, Any]:
        """Get VR scene data for headset."""
        return {
            "model_url": self.tour.vr_url,
            "initial_position": self.viewer_position,
            "pois": [
                {
                    "id": poi.id,
                    "position": poi.position,
                    "content": {
                        "title": poi.title,
                        "description": poi.description,
                        "media": poi.media_url
                    }
                }
                for poi in self.tour.pois
            ],
            "settings": {
                "movement_speed": 2.0,
                "teleport_enabled": True,
                "collision_enabled": True
            }
        }


class ARExperience:
    """AR experience for property tours."""

    def __init__(self, tour: ARTourModel):
        self.tour = tour

    def get_ar_markers(self) -> List[Dict[str, Any]]:
        """Get AR markers for mobile viewing."""
        markers = []

        # Room anchor marker
        markers.append({
            "id": "room_anchor",
            "type": "model",
            "url": self.tour.ar_url,
            "scale": 0.1,  # Adjust for real-world scale
            "position": [0, 0, 0]
        })

        # POI markers
        for poi in self.tour.pois:
            markers.append({
                "id": poi.id,
                "type": "poi",
                "position": poi.position,
                "content": {
                    "title": poi.title,
                    "description": poi.description,
                    "icon": self._get_poi_icon(poi.type)
                }
            })

        return markers

    def _get_poi_icon(self, poi_type: str) -> str:
        """Get appropriate icon for POI type."""
        icons = {
            "appliance": "🔌",
            "feature": "✨",
            "info": "ℹ️"
        }
        return icons.get(poi_type, "📍")


# Global AR/VR service instance
ar_tour_service = ARVirtualTourService()