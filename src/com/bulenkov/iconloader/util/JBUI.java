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

import com.bulenkov.iconloader.IconLoader;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class JBUI {
    private static float scaleFactor = 1.0f;

    static {
        calculateScaleFactor();
    }

    private static void calculateScaleFactor() {
        if (SystemInfo.isMac) {
            scaleFactor = 1.0f;
            return;
        }

        if (System.getProperty("hidpi") != null && !"true".equalsIgnoreCase(System.getProperty("hidpi"))) {
            scaleFactor = 1.0f;
            return;
        }

        UIUtil.initSystemFontData();
        Integer fdata = UIUtil.getSystemFontData();

        int size;
        if (fdata != null) {
            size = fdata;
        } else {
            size = UIManager.getFont("Label.font").getSize();
        }
        setScaleFactor(size / UIUtil.DEF_SYSTEM_FONT_SIZE);
    }

    public static void setScaleFactor(float scale) {
        final String value = System.getProperty("hidpi");
        if (value != null && "false".equalsIgnoreCase(value)) {
            return;
        }

        if (scale < 1.25f) scale = 1.0f;
        else if (scale < 1.5f) scale = 1.25f;
        else if (scale < 1.75f) scale = 1.5f;
        else if (scale < 2f) scale = 1.75f;
        else scale = 2.0f;

        if (SystemInfo.isLinux && scale == 1.25f) {
            //Default UI font size for Unity and Gnome is 15. Scaling factor 1.25f works badly on Linux
            scale = 1f;
        }
        if (scaleFactor == scale) {
            return;
        }

        scaleFactor = scale;
        IconLoader.setScale(scale);
    }

    public static int scale(int i) {
        return Math.round(scaleFactor * i);
    }

    public static float scale(float f) {
        return f * scaleFactor;
    }

    public static boolean isHiDPI() {
        return scaleFactor > 1.0f;
    }

}
