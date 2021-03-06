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

import com.bulenkov.iconloader.util.ImageLoader;
import com.bulenkov.iconloader.util.ImageUtil;
import com.bulenkov.iconloader.util.JBImageIcon;
import com.bulenkov.iconloader.util.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UnusedDeclaration")
public final class IconLoader {

  private IconLoader() { }

  /**
   * Might return null if icon was not found.
   */
  @Nullable
  public static Icon getIcon(URL url) {
    if (url == null) {
      return null;
    }
    Image image = ImageLoader.loadFromUrl(url);
    ImageIcon ii = checkIcon(image, url);

    if(ii == null) {
      return null;
    }

    return new IconWrapper(ii);
  }

  @Nullable
  private static ImageIcon checkIcon(final Image image, @NotNull URL url) {
    if (image == null || image.getHeight(ImageLoader.ourComponent) < 1) { // image wasn't loaded or broken
      return null;
    }

    final ImageIcon icon = new JBImageIcon(image);
    if (!isGoodSize(icon)) {
      return null;
    }

    return icon;
  }

  public static boolean isGoodSize(@NotNull final Icon icon) {
    return icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
  }

  /**
   * Gets (creates if necessary) disabled icon based on the passed one.
   *
   * @return <code>ImageIcon</code> constructed from disabled image of passed icon, null if source icon is wrong
   */
  @Nullable
  public static Icon getDisabledIcon(Icon icon) {
    if (icon == null) return null;

    Icon disabledIcon;
      if (!isGoodSize(icon)) {
        return null;
      }
      final int scale = UIUtil.isRetina() ? 2 : 1;
      @SuppressWarnings("UndesirableClassUsage")
      BufferedImage image = new BufferedImage(scale*icon.getIconWidth(), scale*icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D graphics = image.createGraphics();

      graphics.setColor(UIUtil.TRANSPARENT_COLOR);
      graphics.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
      graphics.scale(scale, scale);
      icon.paintIcon(ImageLoader.ourComponent, graphics, 0, 0);

      graphics.dispose();

      Image img = ImageUtil.filter(image, UIUtil.getGrayFilter());
      if (UIUtil.isRetina()) img = RetinaImage.createFrom(img, ImageLoader.ourComponent);

      disabledIcon = new JBImageIcon(img);
    return new IconWrapper(disabledIcon);
  }

}
