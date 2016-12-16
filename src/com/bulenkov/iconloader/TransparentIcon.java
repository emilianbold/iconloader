/*
 * Copyright 2000-2014 JetBrains s.r.o.
 * Copyright 2016 Emilian Marius Bold
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

import javax.swing.*;
import java.awt.*;

public class TransparentIcon implements Icon {

    private final Icon icon;
    private final float alpha;

    public TransparentIcon(Icon icon) {
        this(icon, 0.5f);
    }

    public TransparentIcon(Icon icon, float alpha) {
        this.icon = icon;
        this.alpha = alpha;
    }

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
        final Graphics2D g2 = (Graphics2D) g;
        final Composite saveComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        icon.paintIcon(c, g2, x, y);
        g2.setComposite(saveComposite);
    }

}
