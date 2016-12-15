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

import com.bulenkov.iconloader.util.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UnusedDeclaration")
public final class IconLoader {
  public static boolean STRICT = false;
  private static boolean USE_DARK_ICONS = UIUtil.isUnderDarcula();

  private static final ImageIcon EMPTY_ICON = new ImageIcon(UIUtil.createImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)) {
    @NonNls
    public String toString() {
      return "Empty icon " + super.toString();
    }
  };

  private static AtomicBoolean ourIsSaveRealIconPath = new AtomicBoolean(false);
  public static final Component ourComponent = new Component() {};

  private IconLoader() { }

  @Deprecated
  public static Icon getIcon(@NotNull final Image image) {
    return new JBImageIcon(image);
  }

  public static void setUseDarkIcons(boolean useDarkIcons) {
    USE_DARK_ICONS = useDarkIcons;
  }

  //TODO[kb] support iconsets
  //public static Icon getIcon(@NotNull final String path, @NotNull final String darkVariantPath) {
  //  return new InvariantIcon(getIcon(path), getIcon(darkVariantPath));
  //}

  @NotNull
  public static Icon getIcon(@NotNull String path, @NotNull final Class aClass) {
    final Icon icon = findIcon(path, aClass);
    if (icon == null) {
      System.err.println("Icon cannot be found in '" + path + "', aClass='" + aClass + "'");
    }
    return icon;
  }

  /**
   * This method is for test purposes only
   */
  static void enableSaveRealIconPath() {
    ourIsSaveRealIconPath.set(true);
  }

  /**
   * Might return null if icon was not found.
   * Use only if you expected null return value, otherwise see {@link IconLoader#getIcon(String, Class)}
   */
  @Nullable
  public static Icon findIcon(@NotNull final String path, @NotNull final Class aClass) {
    return findIcon(path, aClass, false);
  }

  @Nullable
  public static Icon findIcon(@NotNull String path, @NotNull final Class aClass, boolean computeNow) {
    return findIcon(path, aClass, computeNow, STRICT);
  }

  @Nullable
  public static Icon findIcon(@NotNull String path, @NotNull final Class aClass, boolean computeNow, boolean strict) {
    String originalPath = path;

    URL myURL = aClass.getResource(path);
    if (myURL == null) {
      if (strict) throw new RuntimeException("Can't find icon in '" + path + "' near " + aClass);
      return null;
    }
    final Icon icon = findIcon(myURL);
    if (icon instanceof CachedImageIcon) {
      ((CachedImageIcon)icon).myOriginalPath = originalPath;
      ((CachedImageIcon)icon).myClassLoader = aClass.getClassLoader();
    }
    return icon;
  }

  @Nullable
  public static Icon findIcon(URL url) {
    if (url == null) {
      return null;
    }
    CachedImageIcon icon;
      icon = new CachedImageIcon(url);
    return icon;
  }

  @Nullable
  public static Icon findIcon(@NotNull String path, @NotNull ClassLoader classLoader) {
    String originalPath = path;
    if (!StringUtil.startsWithChar(path, '/')) return null;

    final URL url = classLoader.getResource(path.substring(1));
    final Icon icon = findIcon(url);
    if (icon instanceof CachedImageIcon) {
      ((CachedImageIcon)icon).myOriginalPath = originalPath;
      ((CachedImageIcon)icon).myClassLoader = classLoader;
    }
    return icon;
  }

  @Nullable
  private static ImageIcon checkIcon(final Image image, @NotNull URL url) {
    if (image == null || image.getHeight(LabelHolder.ourFakeComponent) < 1) { // image wasn't loaded or broken
      return null;
    }

    final Icon icon = getIcon(image);
    if (icon != null && !isGoodSize(icon)) {
      return EMPTY_ICON;
    }

    return (ImageIcon)icon;
  }

  public static boolean isGoodSize(@NotNull final Icon icon) {
    return icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
  }

  /**
   * Gets (creates if necessary) disabled icon based on the passed one.
   *
   * @return <code>ImageIcon</code> constructed from disabled image of passed icon.
   */
  @Nullable
  public static Icon getDisabledIcon(Icon icon) {
    if (icon == null) return null;

    Icon disabledIcon;
      if (!isGoodSize(icon)) {
        return EMPTY_ICON;
      }
      final int scale = UIUtil.isRetina() ? 2 : 1;
      @SuppressWarnings("UndesirableClassUsage")
      BufferedImage image = new BufferedImage(scale*icon.getIconWidth(), scale*icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D graphics = image.createGraphics();

      graphics.setColor(UIUtil.TRANSPARENT_COLOR);
      graphics.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
      graphics.scale(scale, scale);
      icon.paintIcon(LabelHolder.ourFakeComponent, graphics, 0, 0);

      graphics.dispose();

      Image img = ImageUtil.filter(image, UIUtil.getGrayFilter());
      if (UIUtil.isRetina()) img = RetinaImage.createFrom(img);

      disabledIcon = new JBImageIcon(img);
    return disabledIcon;
  }

  public static Icon getTransparentIcon(@NotNull final Icon icon) {
    return getTransparentIcon(icon, 0.5f);
  }

  public static Icon getTransparentIcon(@NotNull final Icon icon, final float alpha) {
    return new Icon() {

      @Override
      public int getIconHeight() {
        return icon.getIconHeight();
      }

      @Override
      public int getIconWidth() {
        return icon.getIconWidth();
      }

      @Override
      public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        final Graphics2D g2 = (Graphics2D)g;
        final Composite saveComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        icon.paintIcon(c, g2, x, y);
        g2.setComposite(saveComposite);
      }
    };
  }

  /**
   * Gets a snapshot of the icon, immune to changes made by these calls:
   * {@link IconLoader#setUseDarkIcons(boolean)}
   *
   * @param icon the source icon
   * @return the icon snapshot
   */
  @NotNull
  public static Icon getIconSnapshot(@NotNull Icon icon) {
    if (icon instanceof CachedImageIcon) {
      return ((CachedImageIcon)icon).getRealIcon();
    }
    return icon;
  }

  public static final class CachedImageIcon implements Icon {
    private volatile Object myRealIcon;
    public String myOriginalPath;
    private ClassLoader myClassLoader;
    @NotNull
    private URL myUrl;
    private volatile boolean dark;
    private volatile int numberOfPatchers = 0;

    private volatile ImageFilter filter;

    public CachedImageIcon(@NotNull URL url) {
      myUrl = url;
      dark = USE_DARK_ICONS;
    }

    @NotNull
    private synchronized ImageIcon getRealIcon() {
      if (!isValid()) {
        myRealIcon = null;
        dark = USE_DARK_ICONS;
      }
      Object realIcon = myRealIcon;
      if (realIcon instanceof Icon) return (ImageIcon)realIcon;

      ImageIcon icon;
      if (realIcon instanceof Reference) {
        icon = ((Reference<ImageIcon>)realIcon).get();
        if (icon != null) return (ImageIcon)icon;
      }

      Image image = ImageLoader.loadFromUrl(myUrl, true, filter);
      icon = checkIcon(image, myUrl);

      if (icon != null) {
        if (icon.getIconWidth() < 50 && icon.getIconHeight() < 50) {
          realIcon = icon;
        }
        else {
          realIcon = new SoftReference<ImageIcon>(icon);
        }
        myRealIcon = realIcon;
      }

      return icon == null ? EMPTY_ICON : icon;
    }

    private boolean isValid() {
      return dark == USE_DARK_ICONS;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      getRealIcon().paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
      return getRealIcon().getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return getRealIcon().getIconHeight();
    }

    @Override
    public String toString() {
      return myUrl.toString();
    }

  }

  private static class LabelHolder {
    /**
     * To get disabled icon with paint it into the image. Some icons require
     * not null component to paint.
     */
    private static final JComponent ourFakeComponent = new JLabel();
  }
}
