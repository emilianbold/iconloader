/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bulenkov.iconloader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/**
 * @author Konstantin Bulenkov
 */
public class RetinaImage {

  /**
   * Creates a Retina-aware wrapper over a raw image.
   * The raw image should be provided in scale of the Retina default scale factor (2x).
   * The wrapper will represent the raw image in the user coordinate space.
   *
   * @param image the raw image
   * @param observer the raw image observer
   * @return the Retina-aware wrapper
   */
  public static Image createFrom(/* @NotNull */ Image image, ImageObserver observer) {
    int scale = 2;
    int w = image.getWidth(observer);
    int h = image.getHeight(observer);

    Image hidpi = new JBHiDPIScaledImage(image, w / scale, h / scale, BufferedImage.TYPE_INT_ARGB);

    return hidpi;
  }

}
