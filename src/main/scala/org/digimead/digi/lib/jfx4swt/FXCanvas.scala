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

import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.{ Exchanger, TimeUnit }
import javafx.scene.Scene
import org.digimead.digi.lib.jfx4swt.jfx.{ FXAdapter, FXHost, JFaceCanvas }
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{ ControlAdapter, ControlEvent, DisposeEvent, DisposeListener, PaintEvent, PaintListener }
import org.eclipse.swt.graphics.{ GC, Image, ImageData, Point, Rectangle }
import org.eclipse.swt.widgets.{ Canvas, Composite }
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.ref.WeakReference

/**
 * FXCanvas embedded widget.
 *
 * It is much better to have huge pack of small widgets that single big one.
 */
class FXCanvas(parent: Composite, style: Int, val bindSceneSizeToCanvas: Boolean = true) extends Canvas(parent, style) {
  /*
   * Those variables declared as '@volatile protected var' because
   * they will set to null after dispose.
   * Such solution helps VM to GC complex circular references that are used here and there.
   */
  /** Adapter between JavaFX EmbeddedWindow and SWT FXCanvas. */
  @volatile protected var adapterInstance = createAdapter(bindSceneSizeToCanvas)
  @volatile protected var hostInstance = createHost(adapterInstance)
  @volatile protected var stageInstance = createJFaceCanvas(hostInstance)
  @volatile protected var preferredHeight = SWT.DEFAULT
  @volatile protected var preferredWidth = SWT.DEFAULT
  /** List of dispose listeners for JFaceCanvas. */
  protected val disposeListeners = new mutable.ArrayBuffer[JFaceCanvas ⇒ _]() with mutable.SynchronizedBuffer[JFaceCanvas ⇒ _]

  initialize

  /** Get adapter. */
  def adapter = Option(adapterInstance)
  /**
   * Adds the listener to the collection of listeners who will
   * be notified when the widget is disposed. When the widget is
   * disposed, the listener is notified by sending it the
   * <code>widgetDisposed()</code> message within Java FX event thread.
   *
   * @param listener the listener which should be notified when the receiver is disposed
   *
   */
  def addDisposeListener[T](onDispose: JFaceCanvas ⇒ T) = disposeListeners.append(onDispose)
  /** Returns the preferred size of the receiver. */
  override def computeSize(wHint: Int, hHint: Int, changed: Boolean) =
    if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT)
      if (preferredHeight == SWT.DEFAULT || preferredHeight == SWT.DEFAULT)
        super.computeSize(wHint, hHint, changed)
      else
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
  /** Get host. */
  def host = Option(hostInstance)
  /** Redraw. */
  def sceneNeedsRepaint() = host.foreach(_.embeddedScene.foreach(_.entireSceneNeedsRepaint()))
  /** Set scene preferred size. */
  def setPreferredSize(x: Int, y: Int) = Option(hostInstance).foreach { host ⇒
    // Event if preferredWidth/preferredHeight will be inconsistent
    // there will be onHostResize from host.
    preferredWidth = x
    preferredHeight = y
    JFX.exec { host.setPreferredSize(x, y) }
  }

  /** Set scene to stage. */
  def setScene[T](scene: Scene, onReady: JFaceCanvas ⇒ T = FXCanvas.nopCallback) =
    JFX.exec { stage.foreach(_.open(scene, onReady)) }
  /** Get stage. */
  def stage = Option(stageInstance)

  /** Create adapter. */
  protected def createAdapter(bindSceneSizeToCanvas: Boolean) = new Adapter(bindSceneSizeToCanvas)
  /** Create embedded. */
  protected def createJFaceCanvas(host: FXHost) = new JFaceCanvas(WeakReference(host))
  /** Create host. */
  protected def createHost(adapter: Adapter) = new FXHost(WeakReference(adapter))
  /** Initialize FXCanvas widget */
  protected def initialize() {
    checkWidget()
    addPaintListener(adapterInstance)
    addControlListener(adapterInstance)
    addDisposeListener(new DisposeListener {
      def widgetDisposed(e: DisposeEvent) {
        val canvas = e.widget.asInstanceOf[FXCanvas]
        removePaintListener(canvas.adapterInstance)
        removeControlListener(canvas.adapterInstance)
        val disposeListeners = FXCanvas.this.disposeListeners.toIndexedSeq
        FXCanvas.this.disposeListeners.clear()
        stageInstance.close((stage: JFaceCanvas) ⇒ disposeListeners.foreach(_(stage)))
        canvas.adapterInstance = null
        canvas.hostInstance = null
        canvas.stageInstance = null
      }
    })
  }

  class Adapter(val bindSceneSizeToCanvas: Boolean) extends ControlAdapter with FXAdapter with PaintListener {
    private[this] final val paletteData = JFX.paletteData
    private[this] final var imageDataFrameOne = new ImageData(1, 1, 32, paletteData, 4, new Array[Byte](4))
    private[this] final var imageDataFrameTwo = new ImageData(1, 1, 32, paletteData, 4, new Array[Byte](4))
    private[this] final var paintControlToDraw: Array[Byte] = null
    private[this] final var paintControlImageData: ImageData = null
    private[this] final var paintControlOffscreenBounds: Rectangle = null
    private[this] final var paintControlOffscreenImage: Image = null
    private[this] final var paintControlGCOffscreenImage: GC = null
    @volatile private[this] final var hostHeight = 0
    @volatile private[this] final var hostWidth = 0
    private[this] final var display = parent.getDisplay()
    private[this] final val disposeRWL = new ReentrantReadWriteLock()
    private[this] final var paintLastX = 0
    private[this] final var paintLastY = 0
    private[this] final var paintLastWidth = 0
    private[this] final var paintLastHeight = 0

    /**
     * Sent when the size (width, height) of a control changes.
     * The default behavior is to do nothing.
     *
     * @param e an event containing information about the resize
     */
    override def controlResized(e: ControlEvent) = if (bindSceneSizeToCanvas) {
      val rect = FXCanvas.this.getClientArea()
      JFX.exec { Option(hostInstance).foreach(_.setPreferredSize(rect.width, rect.height)) }
    }
    /**
     * Dispose adapter.
     */
    override def dispose() {
      disposeRWL.writeLock().lock()
      try {
        super.dispose()
        if (display != null)
          if (display.getThread() == Thread.currentThread()) {
            if (paintControlOffscreenImage != null)
              paintControlOffscreenImage.dispose()
            if (paintControlGCOffscreenImage != null)
              paintControlGCOffscreenImage.dispose()
            display = null
          } else
            display.asyncExec(new Runnable { def run = Adapter.this.dispose() })
        imageDataFrameOne = null
        imageDataFrameTwo = null
        paintControlToDraw = null
        paintControlImageData = null
        paintControlOffscreenImage = null
        paintControlGCOffscreenImage = null
        paintControlOffscreenBounds = null
      } finally disposeRWL.writeLock() unlock ()
    }
    /**
     * Set container preferred size based on scene content.
     * Called from JavaFX event thread.
     */
    def onHostResize(x: Int, y: Int) = {
      disposeRWL.readLock().lock()
      try if (display != null) {
        hostWidth = x
        hostHeight = y
        if (bindSceneSizeToCanvas) {
          FXCanvas.this.preferredWidth = x
          FXCanvas.this.preferredHeight = y
        }
      } finally disposeRWL.readLock().unlock()
    }
    /**
     * Sent when a paint event occurs for the control.
     *
     * @param e an event containing information about the paint
     */
    // In is about 60ms at 1900x1200 :-( ~15 FPS
    // Linux devbox 3.10.0-gentoo #1 SMP PREEMPT Sat Jul 6 19:42:57 MSK 2013 x86_64 Intel(R) Core(TM) i7-2600K CPU @ 3.40GHz GenuineIntel GNU/Linux
    // GTK Cairo is too slow
    // Pure JavaFX with Java2D thread provides ~30 FPS at the same time. (every 2nd frame is dropped)
    // I see no workaround for this except GLCanvas, but OpenGL is out of scope.
    override def paintControl(event: PaintEvent) {
      if (event.width <= 0 || event.height <= 0)
        return
      paintControlToDraw = frameFull.getAndSet(null)
      // Free shared resource early at the beginning.
      if (paintControlToDraw != null) {
        paintControlImageData = if (frameOne.get() == paintControlToDraw) {
          frameEmpty.set(frameTwo.get())
          if ((hostWidth * hostHeight * 4) != paintControlToDraw.length) {
            null
          } else {
            if (imageDataFrameTwo.data != paintControlToDraw)
              imageDataFrameTwo = new ImageData(hostWidth, hostHeight, 32, paletteData, 4, paintControlToDraw)
            imageDataFrameTwo
          }
        } else {
          frameEmpty.set(frameOne.get())
          if ((hostWidth * hostHeight * 4) != paintControlToDraw.length) {
            null
          } else {
            if (imageDataFrameOne.data != paintControlToDraw)
              imageDataFrameOne = new ImageData(hostWidth, hostHeight, 32, paletteData, 4, paintControlToDraw)
            imageDataFrameOne
          }
        }
      } else if (paintLastX != event.x || paintLastY != event.y || paintLastWidth != event.width || paintLastHeight != event.height)
        // Reset paintControlImageData since there was resize.
        paintControlImageData = null
      // Prepare offscreen image.
      if (paintControlOffscreenImage == null ||
        paintControlOffscreenBounds.width != hostWidth ||
        paintControlOffscreenBounds.height != hostHeight) {
        if (paintControlOffscreenImage != null)
          paintControlOffscreenImage.dispose()
        if (paintControlGCOffscreenImage != null)
          paintControlGCOffscreenImage.dispose()
        if (hostWidth > 0 && hostHeight > 0) {
          // Create the image to fill the canvas.
          paintControlOffscreenImage = new Image(event.display, hostWidth, hostHeight)
          // Set up the offscreen gc.
          paintControlGCOffscreenImage = new GC(paintControlOffscreenImage)
          // Set offscreen bounds
          paintControlOffscreenBounds = paintControlOffscreenImage.getBounds()
        } else {
          paintControlOffscreenImage = null
          paintControlGCOffscreenImage = null
        }
      }
      // Obtain the next frame.
      if (isVisible()) {
        if (paintControlImageData != null) {
          /*
           * ---> HERE <---
           * most CPU and memory hungry
           */
          val imageFrame = new Image(event.display, paintControlImageData)
          // Draw the image offscreen.
          paintControlGCOffscreenImage.setBackground(event.gc.getBackground())
          paintControlGCOffscreenImage.drawImage(imageFrame, 0, 0)
          // Draw the offscreen buffer to the screen.
          event.gc.fillRectangle(event.x, event.y, event.width, event.height)
          event.gc.drawImage(paintControlOffscreenImage, 0, 0)
          imageFrame.dispose()
          // Reset paintControlImageData since there will maybe be resize.
          paintLastX = event.x
          paintLastY = event.y
          paintLastWidth = event.width
          paintLastHeight = event.height
        } else {
          if (paintLastX != event.x || paintLastY != event.y || paintLastWidth != event.width || paintLastHeight != event.height)
            event.gc.fillRectangle(event.x, event.y, event.width, event.height)
          // This is a thread safe call.
          hostInstance.embeddedScene.foreach(_.entireSceneNeedsRepaint())
        }
      }
    }
    def redraw() {
      disposeRWL.readLock().lock()
      try if (display != null) display.asyncExec(new Runnable {
        def run = if (!FXCanvas.this.isDisposed()) FXCanvas.this.redraw(0, 0, hostWidth, hostHeight, false)
      }) finally disposeRWL.readLock().unlock()
    }
    def requestFocus(): Boolean = {
      disposeRWL.readLock().lock()
      try if (display == null) {
        false
      } else {
        val exchanger = new Exchanger[Boolean]()
        display.asyncExec(new Runnable {
          def run {
            if (!FXCanvas.this.isDisposed())
              exchanger.exchange(FXCanvas.this.setFocus(), 100, TimeUnit.MILLISECONDS)
            else
              exchanger.exchange(false, 100, TimeUnit.MILLISECONDS)
          }
        })
        exchanger.exchange(false, JFX.timeout, TimeUnit.MILLISECONDS)
      } finally disposeRWL.readLock().unlock()
    }
    def setEnabled(enabled: Boolean) {
      disposeRWL.readLock().lock()
      try if (display != null) display.asyncExec(new Runnable {
        def run = if (!FXCanvas.this.isDisposed()) FXCanvas.this.setEnabled(enabled)
      }) finally disposeRWL.readLock().unlock()
    }
    def traverseFocusOut(forward: Boolean) = false
  }
}

object FXCanvas {
  val nopCallback = (canvas: JFaceCanvas) ⇒ {}
}
