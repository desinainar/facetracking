package com.google.mlkit.vision.demo.java.facemeshdetector;

import android.util.Log;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.common.Triangle;

import java.util.ArrayList;
import java.util.List;

public class Model3D {
    List<PointF3D> pts;
    List<int[]> tris;
    Model3D(String model_data) {
        String[] lines = model_data.split("\n");
        this.pts = new ArrayList<>();
        this.tris = new ArrayList<>();
        try {
            for(String line : lines) {
                if(line.startsWith("v ")){
                    String[] parts = line.split(" ");
                    float x = Float.parseFloat(parts[1])*200;
                    float y = Float.parseFloat(parts[2])*200;
                    float z = Float.parseFloat(parts[3])*100;
                    this.pts.add(PointF3D.from(x,y,z));
                }
                if(line.startsWith("f")){
                    String[] parts = line.split(" ");
                    int v0 = Integer.parseInt(parts[1].split("/")[0])-1;
                    int v1 = Integer.parseInt(parts[2].split("/")[0])-1;
                    int v2 = Integer.parseInt(parts[3].split("/")[0])-1;
                    int v3 = Integer.parseInt(parts[4].split("/")[0])-1;
                    this.tris.add(new int[]{v0,v1,v2});
                    this.tris.add(new int[]{v0,v2,v3});
                }
            }
        } catch (Exception e) {
            Log.e("PARSE_ERROR", e.toString());
        }
    }
}
