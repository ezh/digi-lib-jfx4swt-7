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

package org.digimead.digi.lib.jfx4swt

import java.util.concurrent.{ CountDownLatch, Executors }
import javafx.animation.FadeTransitionBuilder
import javafx.collections.FXCollections
import javafx.scene.{ Group, Scene }
import javafx.scene.chart.{ NumberAxis, StackedAreaChart, XYChart }
import javafx.util.Duration
import org.digimead.digi.lib.DependencyInjection
import org.digimead.lib.test.LoggingHelper
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{ DisposeEvent, DisposeListener, PaintEvent }
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{ Display, Shell }
import org.scalatest.{ FreeSpec, Matchers }
import scala.concurrent.{ Await, ExecutionContext, Future }

class FXMemoryLeaks extends FreeSpec with Matchers with LoggingHelper {
  lazy val config = org.digimead.digi.lib.default ~ org.digimead.digi.lib.jfx4swt.default
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
  val runtime = Runtime.getRuntime()
  var totalMem = 0L
  var freeMem = 0L
  var usedMem = 0L

  "Check for memory leaks" in {
    showAndWait
    val initialMem = convertToMeg(getMemUsage())
    println("Initial memory usage: " + initialMem)
    for (i ← 0 until 30) {
      showAndWait
      println(s"Iteration ${i} memory usage: " + convertToMeg(getMemUsage()))
    }
    //Thread.sleep(100000)
  }
  "Stress test" in {
    showAndWait
    val initialMem = convertToMeg(getMemUsage())
    println("Initial memory usage: " + initialMem)
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    for (i ← 0 until 10) {
      val futures = for (i ← 0 until 10)
        yield Future {
        println("Show " + i)
        showAndWait
      }
      Await.result(Future.sequence(futures), scala.concurrent.duration.Duration("10 sec"))
      println(s"Iteration ${i} memory usage: " + convertToMeg(getMemUsage()))
    }
    //Thread.sleep(100000)
  }

  def showAndWait = {
    val latch = new CountDownLatch(1)
    Display.getDefault().asyncExec {
      new Runnable {
        def run() = try {
          val shell = new Shell()
          shell.addDisposeListener(new DisposeListener { def widgetDisposed(e: DisposeEvent) = latch.countDown() })
          shell.setLayout(new FillLayout(SWT.VERTICAL))
          val canvas = new FXCanvas(shell, SWT.NONE) {
            override def createAdapter(bindSceneSizeToCanvas: Boolean) = new Adapter(bindSceneSizeToCanvas) {
              var n = 0
              override def paintControl(event: PaintEvent) {
                n += 1
//                println(s"${hashCode()} paintControl $n")
                super.paintControl(event)
                if (n == 15) {
                  println(s"${hashCode()} close")
                  getShell().getDisplay().asyncExec(new Runnable { def run = getShell().close() })
                }
              }
            }
          }
          println("Show shell with " + canvas.adapter.get.hashCode())
          val adapter = JFX.exec {
            canvas.initializeJFX()
            val chart = createChart()
            chart.setAnimated(true)
            val fadeTransition = FadeTransitionBuilder.create()
              .duration(Duration.seconds(3))
              .fromValue(0.0)
              .toValue(1.0)
              .node(chart)
              .autoReverse(true)
              .cycleCount(-1)
              .build()
            val scene = new Scene(chart)
            canvas.setScene(scene, { stage ⇒
              println(stage + " ready for " + canvas.adapter.get.hashCode())
              fadeTransition.play()
            })
            canvas.addDisposeListener { stage ⇒
              fadeTransition.stop()
              scene.rootProperty().setValue(new Group)
            }

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
    latch.await()
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
    ac
  }

  def convertToMeg(numBytes: Long) = (numBytes + (512 * 1024)) / (1024 * 1024)
  def getMemUsage() = {
    System.gc()
    System.runFinalization()
    Thread.sleep(1000)
    System.gc()
    System.runFinalization()
    totalMem = runtime.totalMemory()
    freeMem = runtime.freeMemory()
    totalMem - freeMem
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
