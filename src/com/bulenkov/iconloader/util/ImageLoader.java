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

/**
 * @author Konstantin Bulenkov
 */
public class ImageLoader implements Serializable {

    @Nullable
    public  static InputStream urlStream(String path, boolean original) throws IOException {
      InputStream stream = null;
      URL url = null;
        url = new URL(path);
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
          if (!original) return null;
          connection.addRequestProperty("User-Agent", "IntelliJ");
        }
        stream = connection.getInputStream();
      return stream;
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
    return loadFromUrl(url, UIUtil.isRetina(), null);
  }

  @Nullable
  public static Image loadFromUrl(URL url, boolean retina, ImageFilter filter) {
    String file = url.toString();

    boolean imageIsRetina = false;

    Image image = null;

    if (retina) {
      try {
        image = load(urlStream(getRetina2XName(file), false));
        imageIsRetina = true;
      } catch (IOException ignore) {
      }
    }

    if (image == null) {
      try {
        image = load(urlStream(file, true));
        imageIsRetina = false;
      } catch (IOException ioe) {
      }
    }

    if (image != null) {
        if (filter != null) {
          image = ImageUtil.filter(image, filter);
        }
        if (image != null && UIUtil.isRetina() && imageIsRetina) {
          image = RetinaImage.createFrom(image, ourComponent);
        }
        return image;
    }
    return null;
  }

  public static Image loadFromStream(@NotNull final InputStream inputStream, ImageFilter filter) throws IOException {
    Image image = load(inputStream);
    if(filter != null) {
      image = ImageUtil.filter(image, filter);
    }
    if (image != null && UIUtil.isRetina()) {
      image = RetinaImage.createFrom(image, ourComponent);
    }
    return image;
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

  public static String getRetina2XName(String file) {
    final String name = getNameWithoutExtension(file);
    final String ext = getExtension(file);

    return name + "@2x." + ext;
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
