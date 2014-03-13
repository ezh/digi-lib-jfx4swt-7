/**
 * JFX4SWT - JavaFX library adapter for SWT framework.
 *
 * Copyright (c) 2014 Alexey Aksenov ezh@ezh.msk.ru
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.digimead.digi.lib.jfx4swt.util

import java.util.Random
import javafx.animation.Interpolator
import javafx.beans.binding.{ DoubleBinding, DoubleExpression }
import javafx.geometry.{ BoundingBox, Bounds }
import javafx.scene.{ Node, SnapshotParametersBuilder }
import javafx.scene.canvas.Canvas
import javafx.scene.image.{ Image, WritableImage }
import javafx.scene.paint.{ Color, CycleMethod, ImagePattern, LinearGradient, Paint, Stop }
import javafx.scene.shape.Shape
import org.digimead.digi.lib.jfx4swt.JFX
import org.eclipse.swt.graphics.RGB
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

/**
 * Some functions of this class is based on Util.java of JFXtras
 *
 * Copyright (c) 2011-2013, JFXtras
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
object JFXUtil {
  private[this] implicit lazy val ec = JFX.ec

  lazy val SNAPSHOT_PARAMETER = {
    val builder = SnapshotParametersBuilder.create()
    builder.fill(Color.TRANSPARENT)
    builder.build()
  }

  // Color related utilities

  /** Get Color from RGB value. */
  def fromRGB(rgb: RGB, opacity: Double = 1): Color = Color.rgb(rgb.red, rgb.green, rgb.blue, opacity)
  /**
   * Converts hex color string to color
   * supported formats
   * 0xRRGGBB
   * 0xRRGGBBAA
   * #RRGGBB
   * #RRGGBBAA
   * RRGGBB
   * RRGGBBAA
   * @param COLOR
   * @return color given by hex string
   */
  def fromWebColor(COLOR: String): Color = {
    var red = 0
    var green = 0
    var blue = 0
    var alpha = 1.0d
    if (COLOR.startsWith("0x")) {
      red = Integer.valueOf(COLOR.substring(2, 4), 16);
      green = Integer.valueOf(COLOR.substring(4, 6), 16);
      blue = Integer.valueOf(COLOR.substring(6, 8), 16);
      if (COLOR.length() > 8) {
        alpha = 1.0 / 255.0 * (Integer.valueOf(COLOR.substring(8, 10), 16).toDouble)
      }
    } else if (COLOR.startsWith("#")) {
      red = Integer.valueOf(COLOR.substring(1, 3), 16);
      green = Integer.valueOf(COLOR.substring(3, 5), 16);
      blue = Integer.valueOf(COLOR.substring(5, 7), 16);
      if (COLOR.length() > 7) {
        alpha = 1.0 / 255.0 * (Integer.valueOf(COLOR.substring(7, 9), 16).toDouble)
      }
    } else {
      red = Integer.valueOf(COLOR.substring(0, 2), 16);
      green = Integer.valueOf(COLOR.substring(2, 4), 16);
      blue = Integer.valueOf(COLOR.substring(4, 6), 16);
      if (COLOR.length() > 6) {
        alpha = 1.0 / 255.0 * (Integer.valueOf(COLOR.substring(6, 8), 16).toDouble)
      }
    }
    Color.rgb(red, green, blue, alpha)
  }
  /** Convert Color to CSS value. */
  def toCSSColor(COLOR: Color): String = {
    val CSS_COLOR = new StringBuilder(19)
    CSS_COLOR.append("rgba(")
    CSS_COLOR.append((COLOR.getRed() * 255).toInt).append(", ")
    CSS_COLOR.append((COLOR.getGreen() * 255).toInt).append(", ")
    CSS_COLOR.append((COLOR.getBlue() * 255).toInt).append(", ")
    CSS_COLOR.append(COLOR.getOpacity()).append(");")
    CSS_COLOR.toString()
  }
  /** Convert Color to RGB value. */
  def toRGB(color: Color): RGB = new RGB((color.getRed() * 255).toInt, (color.getGreen() * 255).toInt, (color.getBlue() * 255).toInt)
  /** Convert Color to Web color value. */
  def toWebColor(COLOR: Color): String = {
    var red = Integer.toHexString((COLOR.getRed() * 255).toInt)
    if (red.length() == 1) red = "0" + red
    var green = Integer.toHexString((COLOR.getGreen() * 255).toInt)
    if (green.length() == 1) green = "0" + green
    var blue = Integer.toHexString((COLOR.getBlue() * 255).toInt)
    if (blue.length() == 1) blue = "0" + blue
    "#" + red + green + blue
  }

  def HSLtoRGB(hue: Double, saturation: Double, luminance: Double): Array[Double] = {
    val normalizedHue = ((hue % 360) + 360) % 360;
    val hueN = normalizedHue / 360
    val q =
      if (luminance < 0.5)
        luminance * (1 + saturation)
      else
        (luminance + saturation) - (saturation * luminance)
    val p = 2 * luminance - q;
    val red = Math.max(0, HueToRGB(p, q, hueN + (1.0f / 3.0f)))
    val green = Math.max(0, HueToRGB(p, q, hueN))
    val blue = Math.max(0, HueToRGB(p, q, hueN - (1.0f / 3.0f)))

    Array[Double](red, green, blue)
  }

  def RGBtoHSL(red: Double, green: Double, blue: Double): Array[Double] = {
    //  Minimum and Maximum RGB values are used in the HSL calculations
    val min = Math.min(red, Math.min(green, blue));
    val max = Math.max(red, Math.max(green, blue));

    //  Calculate the Hue
    var hue = 0d
    if (max == min)
      hue = 0
    else if (max == red)
      hue = (((green - blue) / (max - min) / 6.0) + 1) % 1
    else if (max == green)
      hue = ((blue - red) / (max - min) / 6.0) + 1.0 / 3.0
    else if (max == blue)
      hue = ((red - green) / (max - min) / 6.0) + 2.0 / 3.0
    hue = hue * 360

    //  Calculate the Luminance
    val luminance = (max + min) / 2

    //  Calculate the Saturation
    var saturation = 0d
    if (java.lang.Double.compare(max, min) == 0)
      saturation = 0
    else if (luminance <= 0.5)
      saturation = (max - min) / (max + min)
    else
      saturation = (max - min) / (2 - max - min)

    Array[Double](hue, saturation, luminance)
  }

  def HueToRGB(P: Double, Q: Double, _hue: Double): Double = {
    var hue = _hue
    if (hue < 0)
      hue += 1
    if (hue > 1)
      hue -= 1
    if (6 * hue < 1)
      P + ((Q - P) * 6 * hue)
    else if (2 * hue < 1)
      Q
    else if (3 * hue < 2)
      P + ((Q - P) * 6 * ((2.0 / 3.0) - hue))
    else
      P
  }

  def biLinearInterpolateColor(COLOR_UL: Color, COLOR_UR: Color, COLOR_LL: Color, COLOR_LR: Color, FRACTION_X: Float, FRACTION_Y: Float): Color = {
    val INTERPOLATED_COLOR_X1 = Interpolator.LINEAR.interpolate(COLOR_UL, COLOR_UR, FRACTION_X).asInstanceOf[Color]
    val INTERPOLATED_COLOR_X2 = Interpolator.LINEAR.interpolate(COLOR_LL, COLOR_LR, FRACTION_X).asInstanceOf[Color]
    Interpolator.LINEAR.interpolate(INTERPOLATED_COLOR_X1, INTERPOLATED_COLOR_X2, FRACTION_Y).asInstanceOf[Color]
  }

  def darker(COLOR: Color, FRACTION: Double): Color =
    new Color(clamp(0, 1, COLOR.getRed() * (1.0 - FRACTION)),
      clamp(0, 1, COLOR.getGreen() * (1.0 - FRACTION)),
      clamp(0, 1, COLOR.getBlue() * (1.0 - FRACTION)), COLOR.getOpacity())

  def brighter(COLOR: Color, FRACTION: Double): Color =
    new Color(clamp(0, 1, COLOR.getRed() * (1.0 + FRACTION)),
      clamp(0, 1, COLOR.getGreen() * (1.0 + FRACTION)),
      clamp(0, 1, COLOR.getBlue() * (1.0 + FRACTION)),
      COLOR.getOpacity())

  def colorDistance(COLOR1: Color, COLOR2: Color): Double = {
    val DELTA_R = COLOR2.getRed() - COLOR1.getRed()
    val DELTA_G = COLOR2.getGreen() - COLOR1.getGreen()
    val DELTA_B = COLOR2.getBlue() - COLOR1.getBlue()
    Math.sqrt(DELTA_R * DELTA_R + DELTA_G * DELTA_G + DELTA_B * DELTA_B)
  }

  def isDark(COLOR: Color): Boolean = {
    val DISTANCE_TO_WHITE = colorDistance(COLOR, Color.WHITE)
    val DISTANCE_TO_BLACK = colorDistance(COLOR, Color.BLACK)
    DISTANCE_TO_BLACK < DISTANCE_TO_WHITE
  }

  def isBright(COLOR: Color): Boolean = !isDark(COLOR)

  // Snapshot related utilities

  def takeSnapshot(NODE: Node): Image = {
    val img = new WritableImage(NODE.getLayoutBounds().getWidth().toInt, NODE.getLayoutBounds().getHeight().toInt)
    NODE.snapshot(SNAPSHOT_PARAMETER, img)
  }

  // Image pattern related utilities

  def createCarbonPattern(): ImagePattern = {
    val WIDTH = 12d
    val HEIGHT = 12d
    val CANVAS = new Canvas(WIDTH, HEIGHT)
    val CTX = CANVAS.getGraphicsContext2D()

    var offsetY = 0d

    // RULB
    CTX.beginPath()
    CTX.rect(0, 0, WIDTH * 0.5, HEIGHT * 0.5)
    CTX.closePath()

    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.5 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(35, 35, 35)),
      new Stop(1, Color.rgb(23, 23, 23))))
    CTX.fill()

    // RULF
    CTX.beginPath()
    CTX.rect(WIDTH * 0.083333, 0, WIDTH * 0.333333, HEIGHT * 0.416666)
    CTX.closePath()
    offsetY = 0
    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.416666 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(38, 38, 38)),
      new Stop(1, Color.rgb(30, 30, 30))))
    CTX.fill()

    // RLRB
    CTX.beginPath()
    CTX.rect(WIDTH * 0.5, HEIGHT * 0.5, WIDTH * 0.5, HEIGHT * 0.5)
    CTX.closePath()
    offsetY = 0.5
    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.5 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(35, 35, 35)),
      new Stop(1, Color.rgb(23, 23, 23))))
    CTX.fill()

    // RLRF
    CTX.beginPath()
    CTX.rect(WIDTH * 0.583333, HEIGHT * 0.5, WIDTH * 0.333333, HEIGHT * 0.416666)
    CTX.closePath()
    offsetY = 0.5
    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.416666 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(38, 38, 38)),
      new Stop(1, Color.rgb(30, 30, 30))))
    CTX.fill()

    // RURB
    CTX.beginPath()
    CTX.rect(WIDTH * 0.5, 0, WIDTH * 0.5, HEIGHT * 0.5)
    CTX.closePath()
    offsetY = 0
    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.5 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(48, 48, 48)),
      new Stop(1, Color.rgb(40, 40, 40))))
    CTX.fill()

    // RURF
    CTX.beginPath()
    CTX.rect(WIDTH * 0.583333, HEIGHT * 0.083333, WIDTH * 0.333333, HEIGHT * 0.416666)
    CTX.closePath()
    offsetY = 0.083333
    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.416666 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(53, 53, 53)),
      new Stop(1, Color.rgb(45, 45, 45))))
    CTX.fill()

    // RLLB
    CTX.beginPath()
    CTX.rect(0, HEIGHT * 0.5, WIDTH * 0.5, HEIGHT * 0.5)
    CTX.closePath()
    offsetY = 0.5
    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.5 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(48, 48, 48)),
      new Stop(1, Color.rgb(40, 40, 40))))
    CTX.fill()

    // RLLF
    CTX.beginPath()
    CTX.rect(WIDTH * 0.083333, HEIGHT * 0.583333, WIDTH * 0.333333, HEIGHT * 0.416666)
    CTX.closePath()
    offsetY = 0.583333
    CTX.setFill(new LinearGradient(0, offsetY * HEIGHT,
      0, 0.416666 * HEIGHT + offsetY * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(53, 53, 53)),
      new Stop(1, Color.rgb(45, 45, 45))))
    CTX.fill()

    val PATTERN_IMAGE = CANVAS.snapshot(SNAPSHOT_PARAMETER, null)
    var PATTERN = new ImagePattern(PATTERN_IMAGE, 0, 0, WIDTH, HEIGHT, false)

    return PATTERN
  }

  def createPunchedSheetPattern(TEXTURE_COLOR: Color): ImagePattern = {
    val WIDTH = 15d
    val HEIGHT = 15d
    val CANVAS = new Canvas(WIDTH, HEIGHT)
    val CTX = CANVAS.getGraphicsContext2D()

    // BACK
    CTX.beginPath()
    CTX.rect(0, 0, WIDTH, HEIGHT)
    CTX.closePath()
    //CTX.setFill(Color.rgb(29, 33, 35))
    CTX.setFill(TEXTURE_COLOR)
    CTX.fill()

    // ULB
    CTX.beginPath()
    CTX.moveTo(0, HEIGHT * 0.266666)
    CTX.bezierCurveTo(0, HEIGHT * 0.4, WIDTH * 0.066666, HEIGHT * 0.466666, WIDTH * 0.2, HEIGHT * 0.466666)
    CTX.bezierCurveTo(WIDTH * 0.333333, HEIGHT * 0.466666, WIDTH * 0.4, HEIGHT * 0.4, WIDTH * 0.4, HEIGHT * 0.266666)
    CTX.bezierCurveTo(WIDTH * 0.4, HEIGHT * 0.133333, WIDTH * 0.333333, HEIGHT * 0.066666, WIDTH * 0.2, HEIGHT * 0.066666)
    CTX.bezierCurveTo(WIDTH * 0.066666, HEIGHT * 0.066666, 0, HEIGHT * 0.133333, 0, HEIGHT * 0.266666)
    CTX.closePath()
    CTX.setFill(new LinearGradient(0, 0.066666 * HEIGHT,
      0, 0.466666 * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(0, 0, 0)),
      new Stop(1, Color.rgb(68, 68, 68))))
    CTX.fill()

    // ULF
    CTX.beginPath()
    CTX.moveTo(0, HEIGHT * 0.2)
    CTX.bezierCurveTo(0, HEIGHT * 0.333333, WIDTH * 0.066666, HEIGHT * 0.4, WIDTH * 0.2, HEIGHT * 0.4)
    CTX.bezierCurveTo(WIDTH * 0.333333, HEIGHT * 0.4, WIDTH * 0.4, HEIGHT * 0.333333, WIDTH * 0.4, HEIGHT * 0.2)
    CTX.bezierCurveTo(WIDTH * 0.4, HEIGHT * 0.066666, WIDTH * 0.333333, 0, WIDTH * 0.2, 0)
    CTX.bezierCurveTo(WIDTH * 0.066666, 0, 0, HEIGHT * 0.066666, 0, HEIGHT * 0.2)
    CTX.closePath()
    CTX.setFill(TEXTURE_COLOR.darker().darker())
    CTX.fill()

    // LRB
    CTX.beginPath()
    CTX.moveTo(WIDTH * 0.466666, HEIGHT * 0.733333)
    CTX.bezierCurveTo(WIDTH * 0.466666, HEIGHT * 0.866666, WIDTH * 0.533333, HEIGHT * 0.933333, WIDTH * 0.666666, HEIGHT * 0.933333)
    CTX.bezierCurveTo(WIDTH * 0.8, HEIGHT * 0.933333, WIDTH * 0.866666, HEIGHT * 0.866666, WIDTH * 0.866666, HEIGHT * 0.733333)
    CTX.bezierCurveTo(WIDTH * 0.866666, HEIGHT * 0.6, WIDTH * 0.8, HEIGHT * 0.533333, WIDTH * 0.666666, HEIGHT * 0.533333)
    CTX.bezierCurveTo(WIDTH * 0.533333, HEIGHT * 0.533333, WIDTH * 0.466666, HEIGHT * 0.6, WIDTH * 0.466666, HEIGHT * 0.733333)
    CTX.closePath()
    CTX.setFill(new LinearGradient(0, 0.533333 * HEIGHT,
      0, 0.933333 * HEIGHT,
      false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.rgb(0, 0, 0)),
      new Stop(1, Color.rgb(68, 68, 68))))
    CTX.fill()

    // LRF
    CTX.beginPath()
    CTX.moveTo(WIDTH * 0.466666, HEIGHT * 0.666666)
    CTX.bezierCurveTo(WIDTH * 0.466666, HEIGHT * 0.8, WIDTH * 0.533333, HEIGHT * 0.866666, WIDTH * 0.666666, HEIGHT * 0.866666)
    CTX.bezierCurveTo(WIDTH * 0.8, HEIGHT * 0.866666, WIDTH * 0.866666, HEIGHT * 0.8, WIDTH * 0.866666, HEIGHT * 0.666666)
    CTX.bezierCurveTo(WIDTH * 0.866666, HEIGHT * 0.533333, WIDTH * 0.8, HEIGHT * 0.466666, WIDTH * 0.666666, HEIGHT * 0.466666)
    CTX.bezierCurveTo(WIDTH * 0.533333, HEIGHT * 0.466666, WIDTH * 0.466666, HEIGHT * 0.533333, WIDTH * 0.466666, HEIGHT * 0.666666)
    CTX.closePath()
    CTX.setFill(TEXTURE_COLOR.darker().darker())
    CTX.fill()

    val PATTERN_IMAGE = CANVAS.snapshot(SNAPSHOT_PARAMETER, null)
    val PATTERN = new ImagePattern(PATTERN_IMAGE, 0, 0, WIDTH, HEIGHT, false)

    PATTERN
  }

  def createNoiseImage(WIDTH: Double, HEIGHT: Double, COLOR: Color): Image =
    createNoiseImage(WIDTH, HEIGHT, COLOR.darker(), COLOR.brighter(), 30)

  def createNoiseImage(WIDTH: Double, HEIGHT: Double, DARK_COLOR: Color, BRIGHT_COLOR: Color, ALPHA_VARIATION_IN_PERCENT: Double): Image = {
    if (WIDTH <= 0 || HEIGHT <= 0)
      return null
    val alphaVariationInPercent = clamp(0, 100, ALPHA_VARIATION_IN_PERCENT)
    val IMAGE = new WritableImage(WIDTH.toInt, HEIGHT.toInt)
    val PIXEL_WRITER = IMAGE.getPixelWriter()
    val BW_RND = new Random()
    val ALPHA_RND = new Random()
    val ALPHA_START = alphaVariationInPercent / 100 / 2
    val ALPHA_VARIATION = alphaVariationInPercent / 100
    var noiseColor: Color = null
    var noiseAlpha = 0d
    for (y ← 0 until HEIGHT.toInt) {
      for (x ← 0 until WIDTH.toInt) {
        if (BW_RND.nextBoolean()) noiseColor = BRIGHT_COLOR else noiseColor = DARK_COLOR
        noiseAlpha = clamp(0, 1, ALPHA_START + ALPHA_RND.nextDouble() * ALPHA_VARIATION)
        PIXEL_WRITER.setColor(x, y, Color.color(noiseColor.getRed(), noiseColor.getGreen(), noiseColor.getBlue(), noiseAlpha))
      }
    }
    IMAGE
  }

  def applyNoisyBackground(SHAPE: Shape, TEXTURE_COLOR: Color): Paint = {
    val WIDTH = SHAPE.getLayoutBounds().getWidth().toInt
    val HEIGHT = SHAPE.getLayoutBounds().getHeight().toInt
    val IMAGE = new WritableImage(WIDTH, HEIGHT)
    val PIXEL_WRITER = IMAGE.getPixelWriter()
    val BW_RND = new Random()
    val ALPHA_RND = new Random()
    val ALPHA_START = 0.045d
    val ALPHA_VARIATION = 0.09d
    var noiseColor: Color = null
    var noiseAlpha = 0d
    for (y ← 0 until HEIGHT.toInt) {
      for (x ← 0 until WIDTH.toInt) {
        if (BW_RND.nextBoolean()) noiseColor = TEXTURE_COLOR.brighter() else noiseColor = TEXTURE_COLOR.darker()
        noiseAlpha = clamp(0, 1, ALPHA_START + ALPHA_RND.nextDouble() * ALPHA_VARIATION)
        PIXEL_WRITER.setColor(x, y, Color.color(noiseColor.getRed(), noiseColor.getGreen(), noiseColor.getBlue(), noiseAlpha))
      }
    }
    val x = SHAPE.getLayoutBounds().getMinX()
    val y = SHAPE.getLayoutBounds().getMinY()
    val width = SHAPE.getLayoutBounds().getWidth()
    val height = SHAPE.getLayoutBounds().getHeight()
    new ImagePattern(IMAGE, x, y, width, height, false)
  }

  // Misc utilities

  /** Get bounds of the cropped image. */
  /* For example:
   * val bounds = getCroppedBounds(snapshot, 0.01)
   * val bImage = SwingFXUtils.fromFXImage(snapshot, null)
   * val imageRGB = new BufferedImage(bounds.getWidth().toInt, bounds.getHeight().toInt, Transparency.OPAQUE) // Remove alpha-channel from buffered image.
   * val graphics = imageRGB.createGraphics()
   * graphics.drawImage(bImage, -bounds.getMinX().toInt, -bounds.getMinY().toInt, null)
   * graphics.dispose()
   * ImageIO.write(imageRGB, "jpg", new File("......./image.jpg"))
   */
  def getCroppedBounds(source: Image, distance: Double): Bounds = {
    // Get our top-left pixel color as our "baseline" for cropping.
    val pixelReader = source.getPixelReader()
    val baseColor = pixelReader.getColor(0, 0)
    val width = source.getWidth().toInt
    val height = source.getHeight().toInt

    val matrix = new Array[Boolean](width * height)
    for (y ← 0 until height)
      for (x ← 0 until width)
        matrix(width * y + x) = JFXUtil.colorDistance(baseColor, pixelReader.getColor(x, y)) < distance

    // Search for topY
    val topYF = Future {
      var topY = 0
      var run = true
      val iterator = matrix.iterator
      for (y ← 0 until height if run)
        for (x ← 0 until width if run)
          if (!iterator.next()) {
            topY = y
            run = false
          }
      if (run)
        topY = height
      topY
    }
    // Search for bottomY
    val bottomYF = Future {
      var bottomY = 0
      var run = true
      val iterator = matrix.reverseIterator
      for (y ← 0 until height if run)
        for (x ← 0 until width if run)
          if (!iterator.next()) {
            bottomY = height - y
            run = false
          }
      if (run)
        bottomY = width
      bottomY
    }
    // Search for topX
    val topXF = Future {
      var topX = 0
      var run = true
      for (x ← 0 until width if run)
        for (y ← 0 until height if run) {
          if (!matrix(width * y + x)) {
            topX = x
            run = false
          }
        }
      if (run)
        topX = width
      topX
    }
    // Search for bottomX
    val bottomXF = Future {
      var bottomX = 0
      var run = true
      for (x ← width - 1 to 0 by -1 if run)
        for (y ← height - 1 to 0 by -1 if run)
          if (!matrix(width * y + x)) {
            bottomX = x + 1
            run = false
          }
      if (run)
        bottomX = height
      bottomX
    }

    val Seq(topX, topY, bottomX, bottomY) = Await.result(Future.sequence(Seq(topXF, topYF, bottomXF, bottomYF)), Duration.Inf)
    new BoundingBox(topX, topY, math.max(bottomX - topX, 0), math.max(bottomY - topY, 0))
  }

  def getMaxSquareSizeBinding(WIDTH_PROPERTY: DoubleExpression, HEIGHT_PROPERTY: DoubleExpression): DoubleBinding = new DoubleBinding() {
    super.bind(WIDTH_PROPERTY, HEIGHT_PROPERTY)

    override protected def computeValue(): Double =
      if (WIDTH_PROPERTY.get() < HEIGHT_PROPERTY.get()) WIDTH_PROPERTY.get() else HEIGHT_PROPERTY.get()
  }

  def clamp(MIN: Double, MAX: Double, VALUE: Double): Double =
    if (VALUE < MIN) MIN else if (VALUE > MAX) MAX else VALUE
}
