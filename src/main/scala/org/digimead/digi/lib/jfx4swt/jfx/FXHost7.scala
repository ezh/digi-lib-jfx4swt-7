/**
 * JFX4SWT-7 - Java 7 JavaFX library adapter for SWT framework.
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

package org.digimead.digi.lib.jfx4swt.jfx

import com.sun.javafx.embed.HostInterface
import com.sun.javafx.tk.quantum.{ EmbeddedScene, EmbeddedStage }
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock
import javafx.application.Platform
import javafx.scene.{ Group, Scene }
import org.digimead.digi.lib.Disposable
import org.digimead.digi.lib.jfx4swt.JFX
import org.eclipse.swt.SWT
import scala.concurrent.Future
import scala.ref.WeakReference

/**
 * HostInterface implementation that connects scene, stage and adapter together.
 *
 * IMPORTANT JavaFX have HUGE performance loss with embedded content.
 * Those funny code monkeys from Oracle corporation redraw ALL content every time. :-/ Shit? Shit! Shit!!!
 * It is better to create few small contents than one huge.
 *
 * val adapter = new MyFXAdapter
 * val host = new FXHost(adapter)
 * val stage = new FXEmbedded(host)
 * stage.open(scene)
 */
class FXHost7(adapter: WeakReference[FXAdapter]) extends FXHost(adapter) {
  private[this] final var dataToConvert = 0
  private[this] final var destinationPointer = 0
  private[this] final var height = SWT.DEFAULT
  private[this] final var rawPixelsBuf: IntBuffer = null
  private[this] final var rawPixelsBufArray: Array[Int] = null
  @volatile private[this] final var scene: EmbeddedScene = null
  private[this] final var sourcePointer = 0
  @volatile private[this] final var stage: EmbeddedStage = null
  private[this] final var width = SWT.DEFAULT
  private[this] final var repaintLastToDraw: Array[Byte] = null
  private[this] final var pipeBuf: Array[Int] = null
  private[this] final val pipeLock = new ReentrantLock()
  @volatile private[this] var disposed = false
  @volatile protected[this] final var userScene: Scene = null
  // Render thread returns scene with old size rendered on frame with new size!!! ???
  // Somewhere inside Quantum few neighbor operators/locks are not ordered.
  // It is a case of ~1-2ms or less
  @volatile private[this] final var oneMoreFramePlease = true
  private[this] var wantRepaint: Future[_] = null
  private[this] implicit val ec = JFX.ec

  def dispose() {
    disposed = true
    JFX.assertEventThread(true)
    if (Platform.isFxApplicationThread()) {
      pipeLock.lock()
      try {
        if (stage != null)
          disposeEmbeddedStage()
        if (scene != null)
          disposeEmbeddedScene()
        Option(rawPixelsBuf).foreach(_.clear())
        rawPixelsBuf = null
        rawPixelsBufArray = null
        pipeBuf = null
        repaintLastToDraw = null
        height = SWT.DEFAULT
        width = SWT.DEFAULT
        userScene = null
      } finally pipeLock.unlock()
    } else {
      JFX.exec { dispose }
    }
    adapter.get.foreach(_.dispose())
  }
  def embeddedScene = Option(scene)
  def embeddedStage = Option(stage)
  def repaint(): Unit = for {
    adapter ← adapter.get
  } {
    if (rawPixelsBuf == null || scene == null || disposed)
      return
    // 1. We want empty frame to draw.
    repaintLastToDraw = adapter.frameEmpty.getAndSet(null)
    if (repaintLastToDraw != null && pipeLock.tryLock()) {
      try {
        if (scene.getPixels(rawPixelsBuf, width, height)) {
          if (!oneMoreFramePlease) {
            System.arraycopy(rawPixelsBufArray, 0, pipeBuf, 0, rawPixelsBufArray.length)
            // We must assign mutable pointer to immutable value
            // since next iteration will set it to 'null'
            // right in the middle of the Future
            // and there will be NPE
            val toDraw = repaintLastToDraw
            // IMHO This is the best of the worst
            // It is not block event thread while large frame processing ~-20ms
            // Future allow reduce process time to 1-5ms vs 15-25ms
            Future { // Memory overhead is minimal.
              // 2. Convert pixelsBuf to imageData and save it to empty frame.
              pipeLock.lock()
              try {
                destinationPointer = 0
                sourcePointer = 0
                for (y ← 0 until height) {
                  for (x ← 0 until width) {
                    dataToConvert = pipeBuf(sourcePointer)
                    sourcePointer += 1
                    toDraw(destinationPointer) = (dataToConvert & 0xFF).asInstanceOf[Byte] //dst:blue
                    destinationPointer += 1
                    toDraw(destinationPointer) = ((dataToConvert >> 8) & 0xFF).asInstanceOf[Byte] //dst:green
                    destinationPointer += 1
                    toDraw(destinationPointer) = ((dataToConvert >> 16) & 0xFF).asInstanceOf[Byte] //dst:green
                    destinationPointer += 1
                    toDraw(destinationPointer) = 0 //alpha
                    destinationPointer += 1
                  }
                }
                if (adapter.frameFull.compareAndSet(null, toDraw)) {
                  // Full frame are ready for new chunk
                  adapter.redraw()
                } else {
                  // Fail. Put empty frame back.
                  adapter.frameEmpty.set(toDraw)
                }
              } catch {
                case e: ArrayIndexOutOfBoundsException ⇒
                  adapter.frameEmpty.set(repaintLastToDraw)
                  scene.entireSceneNeedsRepaint()
                case e: Throwable ⇒
                  FXHost.log.error("FXHost pipe. " + e.getMessage(), e)
              } finally pipeLock.unlock()
            }
          } else {
            oneMoreFramePlease = false
            JFX.execAsync { // Required
              if (scene != null && stage != null) {
                if (width != SWT.DEFAULT && height != SWT.DEFAULT) {
                  scene.setSize(width, height)
                  stage.setSize(width, height)
                }
                scene.entireSceneNeedsRepaint()
              }
            }
            adapter.frameEmpty.set(repaintLastToDraw)
          }
        } else {
          // Fail. Put empty frame back.
          adapter.frameEmpty.set(repaintLastToDraw)
        }
      } finally pipeLock.unlock()
    } else if (wantRepaint == null) wantRepaint = Future {
      for (i ← 1 to 1000 if adapter.frameEmpty.get == null) // 5 sec
        Thread.sleep(5) // 200 FPS max
      JFX.exec {
        wantRepaint = null
        repaint()
        Future { embeddedScene.foreach(_.entireSceneNeedsRepaint()) }
      }
    }
  }
  def requestFocus(): Boolean = if (disposed) false else adapter.get.map(_.requestFocus()).getOrElse(false)
  def sceneNeedsRepaint() = embeddedScene.foreach { _.entireSceneNeedsRepaint() }
  def setCursor(cursorFrame: com.sun.javafx.cursor.CursorFrame) {}
  def setEmbeddedScene(scene: com.sun.javafx.embed.EmbeddedSceneInterface) {
    if (scene == null && disposed)
      return
    pipeLock.lock()
    try {
      if (this.scene != null)
        disposeEmbeddedScene()
      dataToConvert = 0
      destinationPointer = 0
      rawPixelsBuf = null
      rawPixelsBufArray = null
      sourcePointer = 0
      this.scene = scene.asInstanceOf[com.sun.javafx.tk.quantum.EmbeddedScene]
      pipeBuf = null
      oneMoreFramePlease = true
    } finally pipeLock.unlock()
  }
  def setEmbeddedStage(stage: com.sun.javafx.embed.EmbeddedStageInterface) {
    if (stage == null && disposed)
      return
    if (stage != null && width != SWT.DEFAULT && height != SWT.DEFAULT)
      stage.setSize(width, height)
    if (this.stage != null)
      disposeEmbeddedStage()
    this.stage = stage.asInstanceOf[com.sun.javafx.tk.quantum.EmbeddedStage]
  }
  def setEnabled(enabled: Boolean) = if (!disposed) adapter.get.map(_.setEnabled(enabled))
  /** Set host preferred size. */
  def setPreferredSize(x: Int, y: Int): Unit = for {
    adapter ← adapter.get
  } {
    JFX.assertEventThread()
    if ((width == x && height == y) || x == SWT.DEFAULT || y == SWT.DEFAULT || disposed)
      return
    pipeLock.lock()
    try {
      width = x
      height = y
      val scanline = width * 4
      adapter.frameOne.set(new Array[Byte](scanline * height))
      adapter.frameTwo.set(new Array[Byte](scanline * height))
      adapter.frameEmpty.set(adapter.frameOne.get())
      rawPixelsBuf = IntBuffer.allocate(width * height)
      rawPixelsBufArray = rawPixelsBuf.array()
      pipeBuf = new Array(rawPixelsBufArray.length)
      if (stage != null)
        stage.setSize(width, height)
      if (scene != null)
        scene.setSize(width, height)
      oneMoreFramePlease = true
    } finally pipeLock.unlock()
    adapter.onHostResize(width, height)
  }
  /** Assign user scene to host which is required for calculating preferred size. */
  def setScene(scene: Scene) {
    JFX.assertEventThread()
    userScene = scene
    if (width == SWT.DEFAULT || height == SWT.DEFAULT)
      setPreferredSize(width, height)
  }
  def traverseFocusOut(forward: Boolean): Boolean = if (disposed) false else adapter.get.map(_.traverseFocusOut(forward)).getOrElse(false)

  /** Dispose embedded scene. */
  protected def disposeEmbeddedScene() {
    val scene = this.scene
    this.scene = null
    scene.setFillPaint(null)
    scene.setScene(null)
    scene.setRoot(null)
    scene.setCamera(null)
    scene.setDragStartListener(null)
    scene.setTKDragGestureListener(null)
    scene.setTKDragSourceListener(null)
    scene.setTKDropTargetListener(null)
    scene.setTKSceneListener(null)
    scene.setTKScenePaintListener(null)
    scene.markDirty()
    scene.sceneChanged()
    Disposable.clean(scene) // there is circular references
  }
  /** Dispose embedded stage. */
  protected def disposeEmbeddedStage() {
    val stage = this.stage
    this.stage = null
    this.userScene = null
    stage.close()
    stage.setTKStageListener(null)
    JFX.execAsync { Disposable.clean(stage) }
  }
}
