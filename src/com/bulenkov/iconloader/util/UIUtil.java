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

import com.bulenkov.iconloader.IsRetina;
import com.bulenkov.iconloader.JBHiDPIScaledImage;
import com.bulenkov.iconloader.RetinaImage;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.lang.reflect.Field;

/**
 * @author Konstantin Bulenkov
 */
public class UIUtil {
  public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
  private static volatile Integer ourSystemFontSize;
  public static final float DEF_SYSTEM_FONT_SIZE = 12f; // TODO: consider 12 * 1.33 to compensate JDK's 72dpi font scale

  @Nullable
  public static Integer getSystemFontData() {
    return ourSystemFontSize;
  }

  public static boolean isAppleRetina() {
    return isRetina() && SystemInfo.isAppleJvm;
  }

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


  private static final Ref<Boolean> ourRetina = Ref.create(SystemInfo.isMac ? null : false);

  public static boolean isRetina() {
    synchronized (ourRetina) {
      if (ourRetina.isNull()) {
        ourRetina.set(false); // in case HiDPIScaledImage.drawIntoImage is not called for some reason

        if (SystemInfo.isJavaVersionAtLeast("1.6.0_33") && SystemInfo.isAppleJvm) {
          if (!"false".equals(System.getProperty("ide.mac.retina"))) {
            ourRetina.set(IsRetina.isRetina());
            return ourRetina.get();
          }
        } else if (SystemInfo.isJavaVersionAtLeast("1.7.0_40") && SystemInfo.isOracleJvm) {
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
        }
        ourRetina.set(false);
      }

      return ourRetina.get();
    }
  }

  public static BufferedImage createImage(int width, int height, int type) {
    if (isRetina()) {
      return RetinaImage.create(width, height, type);
    }
    //noinspection UndesirableClassUsage
    return new BufferedImage(width, height, type);
  }


  private static final GrayFilter DEFAULT_GRAY_FILTER = new GrayFilter(true, 65);
  private static final GrayFilter DARCULA_GRAY_FILTER = new GrayFilter(true, 30);

  public static GrayFilter getGrayFilter() {
    return isUnderDarcula() ? DARCULA_GRAY_FILTER : DEFAULT_GRAY_FILTER;
  }

  public static Font getLabelFont() {
    return UIManager.getFont("Label.font");
  }

  public static void initSystemFontData() {
    if (ourSystemFontSize != null) return;

    // With JB Linux JDK the label font comes properly scaled based on Xft.dpi settings.
    Font font = getLabelFont();

    Float forcedScale = null;
    if (SystemInfo.isLinux && !SystemInfo.isJetbrainsJvm) {
      // With Oracle JDK: derive scale from X server DPI
      float scale = getScreenScale();
      if (scale > 1f) {
        forcedScale = scale;
      }
      // Or otherwise leave the detected font. It's undetermined if it's scaled or not.
      // If it is (likely with GTK DE), then the UI scale will be derived from it,
      // if it's not, then IDEA will start unscaled. This lets the users of GTK DEs
      // not to bother about X server DPI settings. Users of other DEs (like KDE)
      // will have to set X server DPI to meet their display.
    }
    else if (SystemInfo.isWindows) {
      //noinspection HardCodedStringLiteral
      Font winFont = (Font)Toolkit.getDefaultToolkit().getDesktopProperty("win.messagebox.font");
      if (winFont != null) {
        font = winFont; // comes scaled
      }
    }
    if (forcedScale != null) {
      // With forced scale, we derive font from a hard-coded value as we cannot be sure
      // the system font comes unscaled.
      font = font.deriveFont(DEF_SYSTEM_FONT_SIZE * forcedScale.floatValue());
    }
    ourSystemFontSize = font.getSize();
  }

  private static float getScreenScale() {
    int dpi = 96;
    try {
      dpi = Toolkit.getDefaultToolkit().getScreenResolution();
    } catch (HeadlessException e) {
    }
    float scale = 1f;
    if (dpi < 120) scale = 1f;
    else if (dpi < 144) scale = 1.25f;
    else if (dpi < 168) scale = 1.5f;
    else if (dpi < 192) scale = 1.75f;
    else scale = 2f;

    return scale;
  }
}
