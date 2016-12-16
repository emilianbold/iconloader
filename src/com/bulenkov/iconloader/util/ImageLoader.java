/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.bulenkov.iconloader.RetinaImage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.ImageFilter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * @author Konstantin Bulenkov
 */
public class ImageLoader implements Serializable {
//  private static final Log LOG = Logger.getLogger("#com.intellij.util.ImageLoader");

  private static class ImageDesc {

    public final String path;
    public final @Nullable Class cls; // resource class if present
    public final float scale; // initial scale factor
    public final boolean original; // path is not altered

    public ImageDesc(String path, Class cls, float scale) {
      this(path, cls, scale, false);
    }

    public ImageDesc(String path, Class cls, float scale, boolean original) {
      this.path = path;
      this.cls = cls;
      this.scale = scale;
      this.original = original;
    }

    @Nullable
    public Image load() throws IOException {
      InputStream stream = null;
      URL url = null;
      if (cls != null) {
        //noinspection IOResourceOpenedButNotSafelyClosed
        stream = cls.getResourceAsStream(path);
        if (stream == null) return null;
      }
      if (stream == null) {
        url = new URL(path);
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
          if (!original) return null;
          connection.addRequestProperty("User-Agent", "IntelliJ");
        }
        stream = connection.getInputStream();
      }
      Image image = ImageLoader.load(stream);
      return image;
    }

    @Override
    public String toString() {
      return path + ", scale: " + scale;
    }
  }

  private static class ImageDescList extends ArrayList<ImageDesc> {
    private ImageDescList() {}

    @Nullable
    public Image load() {
      return load(ImageConverterChain.create());
    }

    @Nullable
    public Image load(@NotNull ImageConverterChain converters) {
      for (ImageDesc desc : this) {
        try {
          Image image = desc.load();
          if (image == null) continue;
//          LOG.debug("Loaded image: " + desc);
          return converters.convert(image, desc);
        }
        catch (IOException ignore) {
        }
      }
      return null;
    }

    public static ImageDescList create(@NotNull String file,
                                       @Nullable Class cls,
                                       boolean dark,
                                       boolean retina)
    {
      ImageDescList vars = new ImageDescList();
      if (retina || dark) {
        final String name = getNameWithoutExtension(file);
        final String ext = getExtension(file);

        if (dark && retina) {
          vars.add(new ImageDesc(name + "@2x_dark." + ext, cls, 2f));
        }

        if (dark) {
          vars.add(new ImageDesc(name + "_dark." + ext, cls, 1f));
        }

        if (retina) {
          vars.add(new ImageDesc(name + "@2x." + ext, cls, 2f));
        }
      }
      vars.add(new ImageDesc(file, cls, 1f, true));
      return vars;
    }
  }

  private interface ImageConverter {
    Image convert(@Nullable Image source, ImageDesc desc);
  }

  private static class ImageConverterChain extends ArrayList<ImageConverter> {
    private ImageConverterChain() {}

    public static ImageConverterChain create() {
      return new ImageConverterChain();
    }

    public ImageConverterChain withFilter(final ImageFilter filter) {
      return with(new ImageConverter() {
        @Override
        public Image convert(Image source, ImageDesc desc) {
          return ImageUtil.filter(source, filter);
        }
      });
    }

    public ImageConverterChain withRetina() {
      return with(new ImageConverter() {
        @Override
        public Image convert(Image source, ImageDesc desc) {
          if (source != null && UIUtil.isRetina() && desc.scale > 1) {
            return RetinaImage.createFrom(source, (int)desc.scale, ourComponent);
          }
          return source;
        }
      });
    }

    public ImageConverterChain with(ImageConverter f) {
      add(f);
      return this;
    }

    public Image convert(Image image, ImageDesc desc) {
      for (ImageConverter f : this) {
        image = f.convert(image, desc);
      }
      return image;
    }
  }

  public static final Component ourComponent = new Component() {
  };

  private static boolean waitForImage(Image image) {
    if (image == null) return false;
    if (image.getWidth(null) > 0) return true;
    MediaTracker mediatracker = new MediaTracker(ourComponent);
    mediatracker.addImage(image, 1);
    try {
      mediatracker.waitForID(1, 5000);
    }
    catch (InterruptedException ex) {
      ex.printStackTrace();
    }
    return !mediatracker.isErrorID(1);
  }

  @Nullable
  public static Image loadFromUrl(@NotNull URL url) {
    return loadFromUrl(url, null);
  }

  @Nullable
  public static Image loadFromUrl(@NotNull URL url, ImageFilter filter) {
    // For any scale factor > 1.0, always prefer retina images, because downscaling
    // retina images provides a better result than upscaling non-retina images.
    final boolean loadRetinaImages = UIUtil.isRetina();

    return ImageDescList.create(url.toString(), null, UIUtil.isUnderDarcula(), loadRetinaImages).load(
      ImageConverterChain.create().
        withFilter(filter).
        withRetina());
  }

  @Nullable
  public static Image loadFromUrl(URL url, boolean dark, boolean retina) {
    return loadFromUrl(url, dark, retina, null);
  }

  @Nullable
  public static Image loadFromUrl(URL url, boolean dark, boolean retina, ImageFilter filter) {
    return ImageDescList.create(url.toString(), null, dark, retina).
      load(ImageConverterChain.create().withFilter(filter).withRetina());
  }

  @Nullable
  public static Image loadFromResource(@NonNls @NotNull String path, @NotNull Class aClass) {
    return ImageDescList.create(path, aClass, UIUtil.isUnderDarcula(), UIUtil.isRetina()).
      load(ImageConverterChain.create().withRetina());
  }

  public static Image loadFromStream(@NotNull final InputStream inputStream) throws IOException {
    return loadFromStream(inputStream, 1);
  }

  public static Image loadFromStream(@NotNull final InputStream inputStream, final int scale) throws IOException {
    return loadFromStream(inputStream, scale, null);
  }

  public static Image loadFromStream(@NotNull final InputStream inputStream, final int scale, ImageFilter filter) throws IOException {
    Image image = load(inputStream);
    ImageDesc desc = new ImageDesc("", null, scale);
    return ImageConverterChain.create().withFilter(filter).withRetina().convert(image, desc);
  }

  private static Image load(@NotNull final InputStream inputStream) throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
        byte[] buffer = new byte[1024];
        while (true) {
          final int n = inputStream.read(buffer);
          if (n < 0) break;
          outputStream.write(buffer, 0, n);
        }
      }
      finally {
        inputStream.close();
      }

      Image image = Toolkit.getDefaultToolkit().createImage(outputStream.toByteArray(), 0, outputStream.size());

      waitForImage(image);

      return image;
  }

  @NotNull
  public static String getNameWithoutExtension(@NotNull String name) {
    int i = name.lastIndexOf('.');
    if (i != -1) {
      name = name.substring(0, i);
    }
    return name;
  }

  @NotNull
  public static String getExtension(@NotNull String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index < 0) return "";
    return fileName.substring(index + 1);
  }
}
