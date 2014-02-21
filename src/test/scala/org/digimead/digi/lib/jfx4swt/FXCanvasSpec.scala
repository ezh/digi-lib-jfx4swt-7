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

import javafx.animation.FadeTransitionBuilder
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.chart.{ NumberAxis, StackedAreaChart, XYChart }
import javafx.util.Duration
import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.jfx4swt.JFX.JFX2interface
import org.digimead.lib.test.LoggingHelper
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{ Display, Shell }
import org.scalatest.{ FreeSpec, Matchers }
import scala.collection.mutable

class FXCanvasSpec extends FreeSpec with Matchers with LoggingHelper {
  lazy val config = org.digimead.digi.lib.default
  val chartSeries = new XYChart.Series[Number, Number]()
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

  "StackedAreaChart should be fine" in {
    val buf = new mutable.ArrayBuffer[Long] with mutable.SynchronizedBuffer[Long]
    Display.getDefault().asyncExec {
      new Runnable {
        def run() = try {
          val shell = new Shell()
          shell.setLayout(new FillLayout(SWT.VERTICAL))
          val canvas = new FXCanvas(shell, SWT.NONE)
          val adapter = JFX.exec {
            val chart = createChart()
            chart.setAnimated(false)
            val scene = new Scene(chart, 500, 500)
            canvas.stage.show()
            canvas.stage.setScene(scene)

            Display.getDefault().asyncExec {
              new Runnable {
                def run() = {
                  canvas.pack()
                  shell.open()
                  shell.setMaximized(true)
                }
              }
            }
          }
        } catch {
          case e: Throwable ⇒ e.printStackTrace()
        }
      }
    }
    Thread.sleep(10000)
    //val len = buf.last.toDouble - buf.head
    //println(s"Total: ${len / 1000} ${buf.length} frames at ${buf.length / (len / 1000)} fps")
  }

  protected def createChart() = {
    val xAxis = new NumberAxis()
    val yAxis = new NumberAxis()
    val ac = new StackedAreaChart[Number, Number](xAxis, yAxis)
    xAxis.setLabel("X Axis")
    yAxis.setLabel("Y Axis")
    ac.setTitle("HelloStackedAreaChart")
    // add starting data
    val data = FXCollections.observableArrayList()
    chartSeries.getData().add(new XYChart.Data(10d, 10d))
    chartSeries.getData().add(new XYChart.Data(25d, 20d))
    chartSeries.getData().add(new XYChart.Data(30d, 15d))
    chartSeries.getData().add(new XYChart.Data(50d, 15d))
    chartSeries.getData().add(new XYChart.Data(80d, 10d))
    val fadeTransition = FadeTransitionBuilder.create()
      .duration(Duration.seconds(3))
      .fromValue(0.0)
      .toValue(1.0)
      .node(ac)
      .autoReverse(true)
      .cycleCount(-1)
      .build()
    fadeTransition.play()
    ac
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

  class TestAdapter extends FXAdapter {
    def redraw() {}
    def requestFocus() = true
    def setEnabled(enabled: Boolean) {}
    def setPreferredSize(x: Int, y: Int) {}
    def traverseFocusOut(forward: Boolean) = false
  }
}
