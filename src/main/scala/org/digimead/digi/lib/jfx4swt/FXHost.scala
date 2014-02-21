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

import com.sun.javafx.embed.{ EmbeddedSceneInterface, HostInterface }
import com.sun.javafx.tk.Toolkit
import com.sun.javafx.tk.quantum.{ EmbeddedScene, EmbeddedStage }
import java.nio.IntBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * HostInterface implementation that connects scene, stage and adapter together.
 *
 * IMPORTANT JavaFX have HUGE performance loss with embedded content.
 * Those funny code monkeys from Oracle corporation redraw ALL content every time. :-/ Shit? Shit! Shit!!!
 * It is better to create few small contents than one huge.
 *
 * val adapter = new MyFXAdapter
 * val host = new FXHost(adapter)
 * val stage = new EmbeddedWindow(host)
 * stage.setScene(scene)
 * stage.show()
 */
class FXHost(adapter: FXAdapter) extends HostInterface {
  private[this] final var dataToConvert = 0
  private[this] final var destinationPointer = 0
  private[this] final var height = 0
  private[this] final var rawPixelsBuf: IntBuffer = null
  private[this] final var rawPixelsBufArray: Array[Int] = null
  private[this] final var scene: EmbeddedScene = null
  private[this] final var sourcePointer = 0
  private[this] final var stage: EmbeddedStage = null
  private[this] final var width = 0
  private[this] final var repaintLastToDraw: Array[Byte] = null

  def dispose() {
    scene = null
    stage = null
    Option(rawPixelsBuf).foreach(_.clear())
    rawPixelsBuf = null
    rawPixelsBufArray = null
    scene = null
    stage = null
    repaintLastToDraw = null
  }
  def repaint(): Unit = {
    if (rawPixelsBuf == null)
      return
    // 1. We want empty frame to draw.
    repaintLastToDraw = adapter.frameEmpty.getAndSet(null)
    if (repaintLastToDraw != null) {
      if (!scene.getPixels(rawPixelsBuf, width, height)) {
        // Fail. Put empty frame back.
        adapter.frameEmpty.set(repaintLastToDraw)
        return
      }
      // 2. Convert pixelsBuf to imageData and save it to empty frame.
      destinationPointer = 0
      sourcePointer = 0
      for (y ← 0 until height) {
        for (x ← 0 until width) {
          dataToConvert = rawPixelsBufArray(sourcePointer)
          sourcePointer += 1
          repaintLastToDraw(destinationPointer) = (dataToConvert & 0xFF).asInstanceOf[Byte] //dst:blue
          destinationPointer += 1
          repaintLastToDraw(destinationPointer) = ((dataToConvert >> 8) & 0xFF).asInstanceOf[Byte] //dst:green
          destinationPointer += 1
          repaintLastToDraw(destinationPointer) = ((dataToConvert >> 16) & 0xFF).asInstanceOf[Byte] //dst:green
          destinationPointer += 1
          repaintLastToDraw(destinationPointer) = 0 //alpha
          destinationPointer += 1
        }
      }
      if (adapter.frameFull.compareAndSet(null, repaintLastToDraw))
        // Full frame are ready for new chunk
        adapter.redraw()
      else
        // Fail. Put empty frame back.
        adapter.frameEmpty.set(repaintLastToDraw)
    }
  }
  def requestFocus(): Boolean = adapter.requestFocus()
  def setCursor(cursorFrame: com.sun.javafx.cursor.CursorFrame) {}
  def setEmbeddedScene(scene: com.sun.javafx.embed.EmbeddedSceneInterface) = {
    dataToConvert = 0
    destinationPointer = 0
    height = 0
    rawPixelsBuf = null
    rawPixelsBufArray = null
    sourcePointer = 0
    width = 0
    this.scene = scene.asInstanceOf[com.sun.javafx.tk.quantum.EmbeddedScene]
  }
  def setEmbeddedStage(stage: com.sun.javafx.embed.EmbeddedStageInterface) = {
    if (stage != null && width != 0 && height != 0)
      stage.setSize(width, height)
    this.stage = stage.asInstanceOf[com.sun.javafx.tk.quantum.EmbeddedStage]
  }
  def setEnabled(enabled: Boolean) = adapter.setEnabled(enabled)
  def setPreferredSize(x: Int, y: Int) {
    if (width == x && height == y)
      return
    width = x
    height = y
    val scanline = width * 4
    adapter.frameOne.set(new Array[Byte](scanline * height))
    adapter.frameTwo.set(new Array[Byte](scanline * height))
    adapter.frameEmpty.set(adapter.frameOne.get())
    rawPixelsBuf = IntBuffer.allocate(width * height)
    rawPixelsBufArray = rawPixelsBuf.array()
    scene.asInstanceOf[EmbeddedSceneInterface].setSize(width, height)
    if (stage != null)
      stage.setSize(width, height)
    adapter.setPreferredSize(x, y)
  }
  def traverseFocusOut(forward: Boolean): Boolean = adapter.traverseFocusOut(forward)
}
