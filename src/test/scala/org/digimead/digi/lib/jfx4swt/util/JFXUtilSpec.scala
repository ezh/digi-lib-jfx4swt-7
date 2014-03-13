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

import com.sun.glass.ui.Application
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import javafx.application.Platform
import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.jfx4swt.JFX.JFX2interface
import org.digimead.lib.test.LoggingHelper
import org.eclipse.swt.widgets.Display
import org.scalatest.{ FreeSpec, Matchers }
import org.digimead.digi.lib.jfx4swt.JFX
import javafx.scene.Scene
import javafx.scene.Group
import javafx.scene.shape.Rectangle
import javafx.scene.layout.Pane
import javafx.embed.swing.SwingFXUtils
import java.awt.image.BufferedImage
import java.awt.Transparency
import javax.imageio.ImageIO
import java.io.File
import javafx.scene.paint.Color

class JFXUtilSpec extends FreeSpec with Matchers with LoggingHelper {
  lazy val config = org.digimead.digi.lib.default
  val thread = new Thread {
    setDaemon(true)
    setName("SWT")
    override def run {
      val display = Display.getDefault()
      while (!display.isDisposed())
        if (!display.readAndDispatch()) display.sleep()
      display.dispose()
    }
  }

  "getCroppedBounds should be OK" in {
    JFX.execNGet {
      info("black on black")
      val rectangle = new Rectangle(10, 10)
      rectangle.setX(6)
      rectangle.setY(6)
      val pane = new Pane()
      pane.getChildren().addAll(rectangle)
      val scene = new Scene(pane, 20, 20)
      val snapshot = JFXUtil.takeSnapshot(pane)
      snapshot.getHeight() should be(20)
      snapshot.getWidth() should be(20)
      val bounds = JFXUtil.getCroppedBounds(snapshot, 0.1)
      bounds.getMinX() should be(20)
      bounds.getMinY() should be(20)
      bounds.getWidth() should be(0)
      bounds.getHeight() should be(0)
    }
    JFX.execNGet {
      info("black on white")
      val rectangle = new Rectangle(10, 10)
      rectangle.setX(6)
      rectangle.setY(6)
      rectangle.setFill(Color.WHITE)
      val pane = new Pane()
      pane.getChildren().addAll(rectangle)
      val scene = new Scene(pane, 20, 20)
      val snapshot = JFXUtil.takeSnapshot(pane)
      snapshot.getHeight() should be(20)
      snapshot.getWidth() should be(20)
      val bounds = JFXUtil.getCroppedBounds(snapshot, 0.1)
      bounds.getMinX() should be(6)
      bounds.getMinY() should be(6)
      bounds.getWidth() should be(10)
      bounds.getHeight() should be(10)
    }
    JFX.execNGet {
      info("black on white horizontal")
      val rectangle = new Rectangle(20, 10)
      rectangle.setX(0)
      rectangle.setY(5)
      rectangle.setFill(Color.WHITE)
      val pane = new Pane()
      pane.getChildren().addAll(rectangle)
      val scene = new Scene(pane, 20, 20)
      val snapshot = JFXUtil.takeSnapshot(pane)
      snapshot.getHeight() should be(20)
      snapshot.getWidth() should be(20)
      val bounds = JFXUtil.getCroppedBounds(snapshot, 0.1)
      bounds.getMinX() should be(0)
      bounds.getMinY() should be(5)
      bounds.getWidth() should be(20)
      bounds.getHeight() should be(10)

      //      val bImage = SwingFXUtils.fromFXImage(snapshot, null)
      //      val imageRGB = new BufferedImage(20, 20, Transparency.OPAQUE) // Remove alpha-channel from buffered image.
      //      val graphics = imageRGB.createGraphics()
      //      graphics.drawImage(bImage, 0, 0, null)
      //      graphics.dispose()
      //      ImageIO.write(imageRGB, "jpg", new File("/tmp/image.jpg"))
    }
    JFX.execNGet {
      info("black on white vertical")
      val rectangle = new Rectangle(10, 20)
      rectangle.setX(5)
      rectangle.setY(0)
      rectangle.setFill(Color.WHITE)
      val pane = new Pane()
      pane.getChildren().addAll(rectangle)
      val scene = new Scene(pane, 20, 20)
      val snapshot = JFXUtil.takeSnapshot(pane)
      snapshot.getHeight() should be(20)
      snapshot.getWidth() should be(20)
      val bounds = JFXUtil.getCroppedBounds(snapshot, 0.1)
      bounds.getMinX() should be(5)
      bounds.getMinY() should be(0)
      bounds.getWidth() should be(10)
      bounds.getHeight() should be(20)
    }
  }

  override def beforeAll(configMap: org.scalatest.ConfigMap) {
    adjustLoggingBeforeAll(configMap)
    DependencyInjection(config, false)
    thread.start()
    JFX.start()
  }
  override def afterAll(configMap: org.scalatest.ConfigMap) {
    JFX.stop()
    Display.getDefault().asyncExec(new Runnable { def run = Display.getDefault().dispose() })
  }
}
