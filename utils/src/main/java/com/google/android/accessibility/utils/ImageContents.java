/*
 * Copyright (C) 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.utils;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.caption.ImageCaptionStorage;
import com.google.android.accessibility.utils.caption.ImageNode;
import com.google.android.accessibility.utils.labeling.Label;
import com.google.android.accessibility.utils.labeling.LabelManager;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A wrapper around LabelManager and ImageCaptionStorage to provide custom labels and the results of
 * image captions.
 */
public class ImageContents {

  private final LabelManager labelManager;
  private final ImageCaptionStorage imageCaptionStorage;
  private @Nullable Locale currentSpeechLocale;

  public ImageContents(LabelManager labelManager, ImageCaptionStorage imageCaptionStorage) {
    this.labelManager = labelManager;
    this.imageCaptionStorage = imageCaptionStorage;
  }

  /**
   * Retrieves custom labels from the database.
   *
   * <p><strong>Note:</strong> Caller is responsible for recycling the node-argument.
   */
  public @Nullable String getLabel(AccessibilityNodeInfoCompat node) {
    if (labelManager == null) {
      return null;
    }
    final Label label = labelManager.getLabelForViewIdFromCache(node.getViewIdResourceName());
    return (label == null || label.getText() == null) ? null : label.getText();
  }

  /**
   * Retrieves the results of image captions from the cache.
   *
   * <p><strong>Note:</strong> Caller is responsible for recycling the node-argument.
   */
  public @Nullable CharSequence getCaptionResult(AccessibilityNodeInfoCompat node) {
    if (imageCaptionStorage == null) {
      return null;
    }
    final @Nullable ImageNode captionResult = imageCaptionStorage.getCaptionResults(node);
    return (captionResult == null || captionResult.getOcrText() == null)
        ? null
        : captionResult.getOcrText();
  }

  /**
   * Retrieves the localized label of the detected icon which matches the specified node.
   *
   * <p><strong>Note:</strong> Caller is responsible for recycling the node-argument.
   */
  public @Nullable CharSequence getDetectedIconLabel(
      Locale locale, AccessibilityNodeInfoCompat node) {
    if (imageCaptionStorage == null) {
      return null;
    }

    // Clears all cached ImageNodes when current speech locale has changed
    if (!locale.equals(currentSpeechLocale)) {
      if (currentSpeechLocale != null) {
        imageCaptionStorage.clearImageNodesCache();
      }
      currentSpeechLocale = locale;
    }

    CharSequence detectedIconLabel = imageCaptionStorage.getDetectedIconLabel(locale, node);
    if (detectedIconLabel == null) {
      ImageNode imageNode = imageCaptionStorage.getCaptionResults(node);
      if (imageNode != null) {
        detectedIconLabel = imageNode.getDetectedIconLabel();
      }
    } else {
      AccessibilityNode wrapNode = AccessibilityNode.obtainCopy(node);
      try {
        imageCaptionStorage.updateDetectedIconLabel(wrapNode, detectedIconLabel);
      } finally {
        AccessibilityNode.recycle("ImageContents.getDetectedIconLabel()", wrapNode);
      }
    }
    return detectedIconLabel;
  }

  /**
   * Checks if the node needs a label.
   *
   * <p><strong>Note:</strong> Caller is responsible for recycling the node-argument.
   */
  public boolean needsLabel(AccessibilityNodeInfoCompat node) {
    return labelManager != null && labelManager.needsLabel(node);
  }
}
