/*
 * Copyright (C) 2016 team-cachebox.de
 *
 * Licensed under the : GNU General Public License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.longri.cachebox3;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.app.beans.SVGIcon;
import org.oscim.awt.AwtBitmap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by Longri on 19.07.16.
 */
public class DesktopRealSvgBitmap extends AwtBitmap {

    private static BufferedImage getBufferdImage(InputStream inputStream, PlatformConnector.SvgScaleType scaleType, float scaleValue) throws IOException {
        synchronized (SVGCache.getSVGUniverse()) {
            try {
                URI uri = SVGCache.getSVGUniverse().loadSVG(inputStream, Integer.toString(inputStream.hashCode()));
                SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(uri);

                float scale = 1;

                switch (scaleType) {

                    case SCALED_TO_WIDTH:
                        scale = scaleValue / diagram.getWidth();
                        break;
                    case SCALED_TO_HEIGHT:
                        scale = scaleValue / diagram.getHeight();
                        break;
                    case DPI_SCALED:
                        scale = CB.getScaledFloat(scaleValue);
                        break;
                }


                float bitmapWidth = diagram.getWidth() * scale;
                float bitmapHeight = diagram.getHeight() * scale;

                SVGIcon icon = new SVGIcon();
                icon.setAntiAlias(true);
                icon.setPreferredSize(new Dimension((int) bitmapWidth, (int) bitmapHeight));
                icon.setScaleToFit(true);
                icon.setSvgURI(uri);
                BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                try {
                    icon.paintIcon(null, bufferedImage.createGraphics(), 0, 0);
                } catch (Exception e) {
                    //return empty image
                    bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                }

                return bufferedImage;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException();
            }
        }
    }


    public DesktopRealSvgBitmap(InputStream inputStream, PlatformConnector.SvgScaleType scaleType, float scaleValue) throws IOException {
        super(getBufferdImage(inputStream, scaleType, scaleValue));
    }

}
