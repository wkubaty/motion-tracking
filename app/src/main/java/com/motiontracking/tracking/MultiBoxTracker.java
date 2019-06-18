/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.motiontracking.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import com.motiontracking.databinding.ActivityMainBinding;
import com.motiontracking.tflite.Classifier;
import com.motiontracking.tflite.Classifier.Recognition;
import com.motiontracking.utils.ImageUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.stream.Collectors;


/** A tracker that handles non-max suppression and matches existing objects to new detections. */
public class MultiBoxTracker {
  private static final float TEXT_SIZE_DIP = 18;
  private static final float MIN_SIZE = 16.0f;
  private static final int[] COLORS = {
    Color.BLUE,
    Color.RED,
    Color.GREEN,
    Color.YELLOW,
    Color.CYAN,
    Color.MAGENTA,
    Color.WHITE,
    Color.parseColor("#55FF55"),
    Color.parseColor("#FFA500"),
    Color.parseColor("#FF8888"),
    Color.parseColor("#AAAAFF"),
    Color.parseColor("#FFFFAA"),
    Color.parseColor("#55AAAA"),
    Color.parseColor("#AA33AA"),
    Color.parseColor("#0D0068")
  };
  private final Queue<Integer> availableColors = new LinkedList<Integer>();
  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
  private final Paint boxPaint = new Paint();
  private final float textSizePx;
  private Matrix frameToCanvasMatrix;
  private int frameWidth;
  private int frameHeight;
  private int sensorOrientation;

  public MultiBoxTracker(final Context context) {
    for (final int color : COLORS) {
      availableColors.add(color);
    }

    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(10.0f);
    boxPaint.setStrokeCap(Cap.ROUND);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
  }

  public synchronized void setFrameConfiguration(
      final int width, final int height, final int sensorOrientation) {
    frameWidth = width;
    frameHeight = height;
    this.sensorOrientation = sensorOrientation;
  }


  public synchronized void trackResults(final List<Recognition> results) {
    processResults(results);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

  public synchronized void draw(final Canvas canvas, ActivityMainBinding binding) {

    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
        Math.min(
            canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
            canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));

    frameToCanvasMatrix =
        ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (int) (multiplier * (rotated ? frameHeight : frameWidth)),
            (int) (multiplier * (rotated ? frameWidth : frameHeight)),
            sensorOrientation,
            false);

    for (final TrackedRecognition recognition : trackedObjects) {
      final RectF trackedPos = new RectF(recognition.location);

      getFrameToCanvasMatrix().mapRect(trackedPos);
      boxPaint.setColor(Color.BLACK);
      boxPaint.setTextSize(textSizePx);
      Paint interiorPaint;
      Paint exteriorPaint;
      interiorPaint = new Paint();
      interiorPaint.setTextSize(textSizePx);
      interiorPaint.setColor(Color.WHITE);
      interiorPaint.setStyle(Style.FILL);
      interiorPaint.setAntiAlias(false);
      interiorPaint.setAlpha(255);

      exteriorPaint = new Paint();
      exteriorPaint.setTextSize(textSizePx);
      exteriorPaint.setColor(Color.BLACK);
      exteriorPaint.setStyle(Style.FILL_AND_STROKE);
      exteriorPaint.setStrokeWidth(textSizePx / 8);
      exteriorPaint.setAntiAlias(false);
      exteriorPaint.setAlpha(255);

      if ("person".equals(recognition.title) && binding.checkboxPerson.isChecked()) {
        boxPaint.setColor(Color.parseColor("#3F51B5"));
      } else if ("bicycle".equals(recognition.title) && binding.checkboxBicycle.isChecked()) {
        boxPaint.setColor(Color.parseColor("#8BC34A"));
      } else if ("car".equals(recognition.title) && binding.checkboxCar.isChecked()) {
        boxPaint.setColor(Color.parseColor("#E91E63"));
      } else if ("truck".equals(recognition.title) && binding.checkboxTruck.isChecked()) {
        boxPaint.setColor(Color.parseColor("#FF9800"));
      } else if ("bus".equals(recognition.title) && binding.checkboxBus.isChecked()) {
        boxPaint.setColor(Color.parseColor("#9C27B0"));
      } else if ("train".equals(recognition.title) && binding.checkboxTrain.isChecked()) {
        boxPaint.setColor(Color.parseColor("#00BCD4"));
      }

      if(boxPaint.getColor()==Color.BLACK){
        return;
      }

      canvas.drawRect(trackedPos, boxPaint);

      final String labelString =
          !TextUtils.isEmpty(recognition.title)
              ? String.format(Locale.ENGLISH,"%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
              : String.format(Locale.ENGLISH,"%.2f", (100 * recognition.detectionConfidence));
      canvas.drawText(labelString + "%" ,trackedPos.left, trackedPos.top, exteriorPaint);
      canvas.drawText(labelString + "%" ,trackedPos.left, trackedPos.top, interiorPaint);

    }
  }
  private boolean isOverlapping(final RectF rect1, final RectF rect2, float ratio){
    Log.d("top, bottom: ",  rect1.top +" " + rect1.bottom);
    float x_diff = ratio*Math.min(rect1.right-rect1.left, rect2.right-rect2.left);
    float y_diff = ratio*Math.min(rect1.top-rect1.bottom, rect2.top-rect2.bottom);

    return !(Math.min(rect1.right, rect2.right) - x_diff < Math.max(rect1.left, rect2.left) ||
            Math.min(rect1.bottom, rect2.bottom) - y_diff < Math.max(rect1.top, rect2.top)); // top is always less than bottom
  }

  private int getArea(RectF location){
    return (int)((location.right - location.left) * (location.bottom - location.top));
  }
  private List<Recognition> removeOverlappingResults(final List<Recognition> rects){
    List<Recognition> sorted = rects; /*rects.stream().sorted(new Comparator<Recognition>() {
      @Override
      public int compare(Recognition o1, Recognition o2) {
        return getArea(o1.getLocation()) - getArea(o2.getLocation());
      }
    }).collect(Collectors.toList());

    for(int i=0; i<sorted.size(); i++){
      Log.d("field: ", getArea(sorted.get(i).getLocation()) + "");
    }
    */
    List<Recognition> filteredResults = new ArrayList<>();


    for(int i=0; i<sorted.size(); i++){
      boolean overlapping = false;
      for(int j=i+1; j<sorted.size(); j++){
        if(sorted.get(i).getTitle().equals(sorted.get(j).getTitle()) && isOverlapping(sorted.get(i).getLocation(), sorted.get(j).getLocation(), 0)){
          overlapping = true;
          break;
        }
      }

      if (!overlapping){
        filteredResults.add(rects.get(i));
      }
    }
    return filteredResults;
  }

  private void processResults(final List<Recognition> results) {
    List<Recognition> filteredResults = removeOverlappingResults(results);

    trackedObjects.clear();

    for (final Classifier.Recognition result : filteredResults) {

      final RectF detectionFrameRect = new RectF(result.getLocation());

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        continue;
      }

      final TrackedRecognition trackedRecognition = new TrackedRecognition();
      trackedRecognition.detectionConfidence = result.getConfidence();
      trackedRecognition.location = new RectF(result.getLocation());
      trackedRecognition.title = result.getTitle();
      trackedRecognition.color = COLORS[trackedObjects.size()];
      trackedObjects.add(trackedRecognition);

      if (trackedObjects.size() >= COLORS.length) {
        break;
      }
    }

  }

  private static class TrackedRecognition {
    RectF location;
    float detectionConfidence;
    int color;
    String title;
  }
}
