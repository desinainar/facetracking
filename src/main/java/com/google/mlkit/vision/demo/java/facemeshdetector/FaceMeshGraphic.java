/*
 * Copyright 2022 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java.facemeshdetector;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.common.Triangle;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.GraphicOverlay.Graphic;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMesh.ContourType;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;
import java.util.ArrayList;
import java.util.List;
//import org.opencv.core.*;

/**
 * Graphic instance for rendering face position and mesh info within the associated graphic overlay
 * view.
 */
public class FaceMeshGraphic extends Graphic {
  private static final int USE_CASE_CONTOUR_ONLY = 999;

  private static final float FACE_POSITION_RADIUS = 8.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;

  private final Paint positionPaint;
  private final Paint boxPaint;
  private volatile FaceMesh faceMesh;
  private Model3D model;
  private final int useCase;
  private float zMin;
  private float zMax;

  @ContourType
  private static final int[] DISPLAY_CONTOURS = {
    FaceMesh.FACE_OVAL,
    FaceMesh.LEFT_EYEBROW_TOP,
    FaceMesh.LEFT_EYEBROW_BOTTOM,
    FaceMesh.RIGHT_EYEBROW_TOP,
    FaceMesh.RIGHT_EYEBROW_BOTTOM,
    FaceMesh.LEFT_EYE,
    FaceMesh.RIGHT_EYE,
    FaceMesh.UPPER_LIP_TOP,
    FaceMesh.UPPER_LIP_BOTTOM,
    FaceMesh.LOWER_LIP_TOP,
    FaceMesh.LOWER_LIP_BOTTOM,
    FaceMesh.NOSE_BRIDGE
  };

  FaceMeshGraphic(GraphicOverlay overlay, FaceMesh faceMesh, Model3D model) {
    super(overlay);

    this.faceMesh = faceMesh;
    this.model = model;
    final int selectedColor = Color.WHITE;

    positionPaint = new Paint();
    positionPaint.setColor(selectedColor);

    boxPaint = new Paint();
    boxPaint.setColor(selectedColor);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

    useCase = PreferenceUtils.getFaceMeshUseCase(getApplicationContext());
  }

  /** Draws the face annotations for position on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    if (faceMesh == null) {
      return;
    }

    // Draws the bounding box.
    RectF rect = new RectF(faceMesh.getBoundingBox());
    // If the image is flipped, the left will be translated to right, and the right to left.
    float x0 = translateX(rect.left);
    float x1 = translateX(rect.right);
    rect.left = min(x0, x1);
    rect.right = max(x0, x1);
    rect.top = translateY(rect.top);
    rect.bottom = translateY(rect.bottom);
    canvas.drawRect(rect, boxPaint);

    // Draw face mesh
    List<FaceMeshPoint> points =
        useCase == USE_CASE_CONTOUR_ONLY ? getContourPoints(faceMesh) : faceMesh.getAllPoints();
    List<Triangle<FaceMeshPoint>> triangles = faceMesh.getAllTriangles();

    zMin = Float.MAX_VALUE;
    zMax = Float.MIN_VALUE;
    for (FaceMeshPoint point : points) {
      zMin = min(zMin, point.getPosition().getZ());
      zMax = max(zMax, point.getPosition().getZ());
    }

    PointF3D top = points.get(10).getPosition();
    PointF3D bot = points.get(152).getPosition();
    PointF3D top_left = points.get(3).getPosition();
    PointF3D top_right = points.get(248).getPosition();
    PointF3D nose = points.get(4).getPosition();
    float scale = (float)Math.sqrt((Math.pow(top_right.getX() - top_left.getX(),2) + Math.pow(top_right.getY() - top_left.getY(),2) + Math.pow(top_right.getZ() - top_left.getZ(),2))) / 28.f;
    float dx = translateX((top_left.getX() + top_right.getX() + nose.getX()) / 3.f);
    float dy = translateY((top_left.getY() + top_right.getY() + nose.getY()) / 3.f);
    float dz = (top_left.getZ() + top_right.getZ() + nose.getZ()) / 3.f;
    float y_tangent = top_right.getX() - top_left.getX();
    float y_cotangent = top_right.getZ() - top_left.getZ();
    float ry = (float)Math.atan2(y_cotangent, y_tangent);
    float x_tangent = top.getY() - bot.getY();
    float x_cotangent = top.getZ() - bot.getZ();
    float rx = (float)Math.atan2(x_cotangent, x_tangent);
    float z_tangent = bot.getY() - top.getY();
    float z_cotangent = bot.getX() - top.getX();
    float rz = -(float)Math.atan2(z_cotangent,z_tangent);

//    for (FaceMeshPoint point : points) {
//      FaceMeshPoint point = points.get(i);
//      updatePaintColorByZValue(
//          positionPaint,
//          canvas,
//          /* visualizeZ= */ true,
//          /* rescaleZForVisualization= */ true,
//          point.getPosition().getZ(),
//          zMin,
//          zMax);
//      canvas.drawCircle(
//          translateX(point.getPosition().getX()),
//          translateY(point.getPosition().getY()),
//          FACE_POSITION_RADIUS,
//          positionPaint);
//    }

//    if (useCase == FaceMeshDetectorOptions.FACE_MESH) {
//      // Draw face mesh triangles
//      for (Triangle<FaceMeshPoint> triangle : triangles) {
//        List<FaceMeshPoint> faceMeshPoints = triangle.getAllPoints();
//        PointF3D point1 = faceMeshPoints.get(0).getPosition();
//        PointF3D point2 = faceMeshPoints.get(1).getPosition();
//        PointF3D point3 = faceMeshPoints.get(2).getPosition();
//
//        drawLine(canvas, point1, point2);
//        drawLine(canvas, point2, point3);
//        drawLine(canvas, point3, point1);
//      }
//    }
//    for (Triangle<FaceMeshPoint> tri : triangles) {
//      List<FaceMeshPoint> pts = tri.getAllPoints();
//      PointF3D point1 = pts.get(0).getPosition();
//      PointF3D point2 = pts.get(1).getPosition();
//      PointF3D point3 = pts.get(2).getPosition();
//
//      drawLine(canvas, point1, point2);
//      drawLine(canvas, point2, point3);
//      drawLine(canvas, point3, point1);
//    }
    for (int[] tri : this.model.tris) {
      PointF3D p1 = transform(this.model.pts.get(tri[0]), scale, dx, dy, dz, rx, ry, rz);
      PointF3D p2 = transform(this.model.pts.get(tri[1]), scale, dx, dy, dz, rx, ry, rz);
      PointF3D p3 = transform(this.model.pts.get(tri[2]), scale, dx, dy, dz, rx, ry, rz);
      PointF3D v1 = PointF3D.from(p1.getX() - p2.getX(), p1.getY() - p2.getY(), p1.getZ() - p2.getZ());
      PointF3D v2 = PointF3D.from(p1.getX() - p3.getX(), p1.getY() - p3.getY(), p1.getZ() - p3.getZ());
      PointF3D cross = PointF3D.from(v1.getY() * v2.getY() - v1.getZ() * v2.getY(), v1.getZ() * v2.getX() - v1.getX() * v2.getZ(), v1.getX() * v2.getY() - v2.getX() * v1.getZ());
      float mag = (float)Math.sqrt(Math.pow(cross.getX(),2) + Math.pow(cross.getY(),2) + Math.pow(cross.getZ(),2));
      float z = cross.getZ() / mag;
      drawTri(canvas,p1,p2,p3,positionPaint);
    }
  }

  private PointF3D transform(PointF3D pt, float scale, float dx, float dy, float dz, float x_rot, float y_rot, float z_rot) {
    float sx = pt.getX() * scale;
    float sy = pt.getY() * scale;
    float sz = pt.getZ() * scale;
    float rx1 = (float)(sx * Math.cos(y_rot) + sz * Math.sin(y_rot));
    float ry1 = sy;
    float rz1 = (float)(-sx * Math.sin(y_rot) + sz * Math.cos(y_rot));
    float rx2 = rx1;
    float ry2 = (float)(ry1 * Math.cos(x_rot) + rz1 * Math.sin(x_rot));
    float rz2 = (float)(-ry1 * Math.sin(x_rot) + rz1 * Math.cos(x_rot));
    float rx3 = (float)(rx2 * Math.cos(z_rot) + ry2 * Math.sin(z_rot));
    float ry3 = (float)(-rx2 * Math.sin(z_rot) + ry2 * Math.cos(z_rot));
    float rz3 = rz2;
    return PointF3D.from(
            rx3 + dx,
            ry3 + dy,
            rz3 + dz
    );
  }

  private List<FaceMeshPoint> getContourPoints(FaceMesh faceMesh) {
    List<FaceMeshPoint> contourPoints = new ArrayList<>();
    for (int type : DISPLAY_CONTOURS) {
      contourPoints.addAll(faceMesh.getPoints(type));
    }
    return contourPoints;
  }

  private void drawLine(Canvas canvas, PointF3D point1, PointF3D point2) {
    updatePaintColorByZValue(
        positionPaint,
        canvas,
        /* visualizeZ= */ true,
        /* rescaleZForVisualization= */ true,
        (point1.getZ() + point2.getZ()) / 2,
        zMin,
        zMax);
    canvas.drawLine(
        translateX(point1.getX()),
        translateY(point1.getY()),
        translateX(point2.getX()),
        translateY(point2.getY()),
        positionPaint);
  }
}
