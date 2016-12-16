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

package com.bulenkov.iconloader.util;

import com.bulenkov.iconloader.JBHiDPIScaledImage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Konstantin Bulenkov
 */
public class UIUtil {
  public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);


  public static boolean isUnderDarcula() {
    return UIManager.getLookAndFeel().getName().equals("Darcula");
  }

  public static void drawImage(Graphics g, Image image, int x, int y, ImageObserver observer) {
    if (image instanceof JBHiDPIScaledImage) {
      final Graphics2D newG = (Graphics2D) g.create(x, y, image.getWidth(observer), image.getHeight(observer));
      newG.scale(0.5, 0.5);
      Image img = ((JBHiDPIScaledImage) image).getDelegate();
      if (img == null) {
        img = image;
      }
      newG.drawImage(img, 0, 0, observer);
      newG.scale(1, 1);
      newG.dispose();
    } else {
      g.drawImage(image, x, y, observer);
    }
  }

  private static boolean isMac() {
    return System.getProperty("os.name").toLowerCase().startsWith("mac");
  }

  private static AtomicBoolean ourRetina = isMac() ? null : new AtomicBoolean(false);

  public static synchronized  boolean isRetina() {
      if (ourRetina == null) {
        ourRetina = new AtomicBoolean();
        ourRetina.set(false); // in case HiDPIScaledImage.drawIntoImage is not called for some reason

          GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
          final GraphicsDevice device = env.getDefaultScreenDevice();
          try {
            Field field = device.getClass().getDeclaredField("scale");
            if (field != null) {
              field.setAccessible(true);
              Object scale = field.get(device);
              if (scale instanceof Integer && (Integer) scale == 2) {
                ourRetina.set(true);
                return true;
              }
            }
          } catch (Exception ignore) {
          }
        ourRetina.set(false);
      }

      return ourRetina.get();
  }

  private static final GrayFilter DEFAULT_GRAY_FILTER = new GrayFilter(true, 65);
  private static final GrayFilter DARCULA_GRAY_FILTER = new GrayFilter(true, 30);

  public static GrayFilter getGrayFilter() {
    return isUnderDarcula() ? DARCULA_GRAY_FILTER : DEFAULT_GRAY_FILTER;
  }

}
