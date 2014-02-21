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

package org.digimead.digi.lib.jfx4swt

import com.sun.javafx.stage.EmbeddedWindow
import java.util.concurrent.{ CountDownLatch, Exchanger, TimeUnit }
import javafx.scene.Scene
import org.digimead.digi.lib.jfx4swt.JFX.JFX2interface
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{ ControlAdapter, ControlEvent, DisposeEvent, DisposeListener, PaintEvent, PaintListener }
import org.eclipse.swt.graphics.{ GC, Image, ImageData, Point, Rectangle }
import org.eclipse.swt.widgets.{ Canvas, Composite }

/**
 * FXCanvas embedded widget.
 *
 * It is much better to have huge pack of small widgets that single big one.
 */
class FXCanvas(parent: Composite, style: Int) extends Canvas(parent, style) {
  /** Adapter between JavaFX EmbeddedWindow and SWT FXCanvas. */
  lazy val adapter = new Adapter
  lazy val host = new FXHost(adapter)
  lazy val stage = new EmbeddedWindow(host)
  protected var preferredHeight = 0
  protected var preferredWidth = 0

  initialize
  override def computeSize(wHint: Int, hHint: Int, changed: Boolean) =
    if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT)
      new Point(preferredWidth, preferredHeight)
    else {
      val size = super.computeSize(wHint, hHint, changed)
      if (wHint == SWT.DEFAULT)
        new Point(preferredWidth, size.y)
      else if (hHint == SWT.DEFAULT)
        new Point(size.x, preferredHeight)
      else
        size
    }

  def initialize() {
    checkWidget()
    addPaintListener(adapter)
    addControlListener(adapter)
    addDisposeListener(new DisposeListener {
      def widgetDisposed(e: DisposeEvent) {
        val canvas = e.widget.asInstanceOf[FXCanvas]
        JFX.execNGet {
          canvas.stage.hide()
          canvas.stage.setScene(null)
          canvas.host.dispose()
        }
        removePaintListener(canvas.adapter)
        addControlListener(canvas.adapter)
        canvas.adapter.dispose()
      }
    })
  }
  protected def setPreferredSize(x: Int, y: Int) {
    preferredWidth = x
    preferredHeight = y
  }

  class Adapter extends ControlAdapter with FXAdapter with PaintListener {
    private[this] final val paletteData = JFX.paletteData
    private[this] final var imageDataFrameOne = new ImageData(1, 1, 32, paletteData, 4, new Array[Byte](4))
    private[this] final var imageDataFrameTwo = new ImageData(1, 1, 32, paletteData, 4, new Array[Byte](4))
    private[this] final var preferredHeight = 0
    private[this] final var preferredWidth = 0
    private[this] final var paintControlToDraw: Array[Byte] = null
    private[this] final var paintControlImageData: ImageData = null
    private[this] final var paintControlOffscreenImage: Image = null
    private[this] final var paintControlGCOffscreenImage: GC = null
    private[this] final var paintControlBounds: Rectangle = null

    /**
     * Sent when the size (width, height) of a control changes.
     * The default behavior is to do nothing.
     *
     * @param e an event containing information about the resize
     */
    override def controlResized(e: ControlEvent) {
      val rect = FXCanvas.this.getClientArea()
      JFX.exec { host.setPreferredSize(rect.width, rect.height) }
    }
    /**
     * Dispose adapter.
     */
    override def dispose() {
      super.dispose()
      if (paintControlOffscreenImage != null)
        paintControlOffscreenImage.dispose()
      if (paintControlGCOffscreenImage != null)
        paintControlGCOffscreenImage.dispose()
      imageDataFrameOne = null
      imageDataFrameTwo = null
      preferredHeight = 0
      preferredWidth = 0
      paintControlToDraw = null
      paintControlImageData = null
      paintControlOffscreenImage = null
      paintControlGCOffscreenImage = null
      paintControlBounds = null
    }
    /**
     * Sent when a paint event occurs for the control.
     *
     * @param e an event containing information about the paint
     */
    // In is about 60ms at 1900x1200 GTK Intel(R) Core(TM) i7-2600K CPU @ 3.40GHz :-( ~15 FPS
    // GTK Cairo is too slow
    // Pure JavaFX with Java2D thread provides ~30 FPS at the same time. (every 2nd frame dropped)
    // I saw no workaround for this except GLCanvas, but OpenGL is out of scope
    override def paintControl(event: PaintEvent) {
      paintControlToDraw = frameFull.getAndSet(null)
      if (paintControlToDraw != null) {
        paintControlImageData = if (frameOne.get() == paintControlToDraw) {
          frameEmpty.set(frameTwo.get())
          if (imageDataFrameTwo.data != paintControlToDraw)
            imageDataFrameTwo = new ImageData(preferredWidth, preferredHeight, 32, paletteData, 4, paintControlToDraw)
          imageDataFrameTwo
        } else {
          frameEmpty.set(frameOne.get())
          if (imageDataFrameOne.data != paintControlToDraw)
            imageDataFrameOne = new ImageData(preferredWidth, preferredHeight, 32, paletteData, 4, paintControlToDraw)
          imageDataFrameOne
        }
        // Prepare offscreen image.
        paintControlBounds = getBounds()
        if (paintControlOffscreenImage == null || paintControlOffscreenImage.getBounds() != paintControlBounds) {
          if (paintControlOffscreenImage != null)
            paintControlOffscreenImage.dispose()
          if (paintControlGCOffscreenImage != null)
            paintControlGCOffscreenImage.dispose()
          // Create the image to fill the canvas.
          paintControlOffscreenImage = new Image(event.display, getBounds())
          // Set up the offscreen gc.
          paintControlGCOffscreenImage = new GC(paintControlOffscreenImage)
        }

        // Obtain the next frame.
        val imageFrame = new Image(event.display, paintControlImageData)
        // Draw the image offscreen.
        paintControlGCOffscreenImage.setBackground(event.gc.getBackground())
        paintControlGCOffscreenImage.drawImage(imageFrame, 0, 0)
        // Draw the offscreen buffer to the screen.
        event.gc.drawImage(paintControlOffscreenImage, 0, 0)
        imageFrame.dispose()
      }
    }
    def setPreferredSize(x: Int, y: Int) = if (!parent.isDisposed()) {
      val execLatch = new CountDownLatch(1)
      parent.getDisplay().asyncExec(new Runnable {
        def run {
          if (!FXCanvas.this.isDisposed()) {
            preferredWidth = x
            preferredHeight = y
            FXCanvas.this.setPreferredSize(x, y)
          }
          execLatch.countDown()
        }
      })
      execLatch.await(JFX.timeout, TimeUnit.MILLISECONDS)
    }
    def redraw() = if (!parent.isDisposed())
      parent.getDisplay().asyncExec(new Runnable {
        def run {
          if (!FXCanvas.this.isDisposed())
            FXCanvas.this.redraw()
        }
      })
    def requestFocus(): Boolean = if (parent.isDisposed()) false else {
      val exchanger = new Exchanger[Boolean]()
      parent.getDisplay().asyncExec(new Runnable {
        def run {
          if (!FXCanvas.this.isDisposed())
            exchanger.exchange(FXCanvas.this.setFocus(), 100, TimeUnit.MILLISECONDS)
          else
            exchanger.exchange(false, 100, TimeUnit.MILLISECONDS)
        }
      })
      exchanger.exchange(false, JFX.timeout, TimeUnit.MILLISECONDS)
    }
    def setEnabled(enabled: Boolean) = if (!parent.isDisposed())
      parent.getDisplay().asyncExec(new Runnable {
        def run {
          if (!FXCanvas.this.isDisposed())
            FXCanvas.this.setEnabled(enabled)
        }
      })
    def traverseFocusOut(forward: Boolean) = false
  }
}
