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

import java.util.concurrent.CountDownLatch
import javafx.animation.FadeTransitionBuilder
import javafx.collections.FXCollections
import javafx.geometry.VPos
import javafx.scene.{ Group, Scene }
import javafx.scene.chart.{ NumberAxis, StackedAreaChart, XYChart }
import javafx.scene.layout.Pane
import javafx.scene.text.{ Font, FontWeight, Text, TextBuilder }
import javafx.util.{ Builder, Duration }
import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.jfx4swt.JFX.JFX2interface
import org.digimead.digi.lib.jfx4swt.jfx.FXAdapter
import org.digimead.lib.test.LoggingHelper
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{ Display, Shell }
import org.mockito.Mockito.{ spy, verify }
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
  val runtime = Runtime.getRuntime()
  var totalMem = 0L
  var freeMem = 0L
  var usedMem = 0L

  "Animated StackedAreaChart should be fine" in {
    val buf = new mutable.ArrayBuffer[Long] with mutable.SynchronizedBuffer[Long]
    Display.getDefault().asyncExec {
      new Runnable {
        def run() = try {
          val shell = new Shell()
          shell.setLayout(new FillLayout(SWT.VERTICAL))
          val canvas = new FXCanvas(shell, SWT.NONE, false) {
            override def createAdapter(bindSceneSizeToCanvas: Boolean, antiFreeze: Boolean) = new Adapter(bindSceneSizeToCanvas, antiFreeze) {
              override def paintControl(event: org.eclipse.swt.events.PaintEvent) {
                buf.append(System.currentTimeMillis())
                super.paintControl(event)
              }
            }
          }
          canvas.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE))
          val adapter = JFX.exec {
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
            fadeTransition.play()
            val scene = new Scene(chart, 500, 500)
            canvas.setScene(scene)

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
    val t = new Thread {
      setDaemon(true)
      override def run {
        while (true) {
          totalMem = runtime.totalMemory()
          freeMem = runtime.freeMemory()
          usedMem = totalMem - freeMem
          println(s"USED: ${convertToMeg(usedMem)} FREE ${convertToMeg(freeMem)} TOTAL ${convertToMeg(totalMem)}")
          Thread.sleep(300)
        }
      }
    }
    //t.start
    Thread.sleep(10000)
    //t.interrupt()
    System.gc()
    System.runFinalization()
    totalMem = runtime.totalMemory()
    freeMem = runtime.freeMemory()
    usedMem = totalMem - freeMem
    println(s"AFTER GC USED: ${convertToMeg(usedMem)} FREE ${convertToMeg(freeMem)} TOTAL ${convertToMeg(totalMem)}")
    val len = buf.last.toDouble - buf.head
    println(s"Total: ${len / 1000} ${buf.length} frames at ${buf.length / (len / 1000)} fps")
  }

  "Static StackedAreaChart should be fine" in {
    val buf = new mutable.ArrayBuffer[Long] with mutable.SynchronizedBuffer[Long]
    Display.getDefault().asyncExec {
      new Runnable {
        def run() = try {
          val shell = new Shell()
          shell.setLayout(new FillLayout(SWT.VERTICAL))
          val canvas = new FXCanvas(shell, SWT.NONE) /* {
            override def createAdapter(bindSceneSizeToCanvas: Boolean) = new Adapter(bindSceneSizeToCanvas) {
              override def paintControl(event: org.eclipse.swt.events.PaintEvent) {
                buf.append(System.currentTimeMillis())
                super.paintControl(event)
              }
            }
          }*/
          val adapter = JFX.exec {
            val chart = createChart()
            chart.setAnimated(false)
            val scene = new Scene(chart, 500, 500)
            canvas.setScene(scene)

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
    val t = new Thread {
      setDaemon(true)
      override def run {
        while (true) {
          totalMem = runtime.totalMemory()
          freeMem = runtime.freeMemory()
          usedMem = totalMem - freeMem
          println(s"USED: ${convertToMeg(usedMem)} FREE ${convertToMeg(freeMem)} TOTAL ${convertToMeg(totalMem)}")
          Thread.sleep(300)
        }
      }
    }
    //t.start
    Thread.sleep(10000)
    //t.interrupt()
    System.gc()
    System.runFinalization()
    totalMem = runtime.totalMemory()
    freeMem = runtime.freeMemory()
    usedMem = totalMem - freeMem
    println(s"AFTER GC USED: ${convertToMeg(usedMem)} FREE ${convertToMeg(freeMem)} TOTAL ${convertToMeg(totalMem)}")
    //val len = buf.last.toDouble - buf.head
    //println(s"Total: ${len / 1000} ${buf.length} frames at ${buf.length / (len / 1000)} fps")
  }

  "Scene without size should be fine" in {
    val latch = new CountDownLatch(1)
    @volatile var adapter1: FXAdapter = null
    Display.getDefault().asyncExec {
      new Runnable {
        def run() = try {
          val shell = new Shell()
          shell.setLayout(new FillLayout(SWT.VERTICAL))
          val canvas = new FXCanvas(shell, SWT.NONE, false) {
            override def createAdapter(bindSceneSizeToCanvas: Boolean, antiFreeze: Boolean) = {
              adapter1 = spy(new Adapter(bindSceneSizeToCanvas, antiFreeze))
              adapter1.asInstanceOf[this.Adapter]
            }
          }
          val adapter = JFX.exec {
            val text = <>[TextBuilder[_], Text](TextBuilder.create()) { b ⇒
              b.text("1234")
              b.font(Font.font(null, FontWeight.BOLD, 100))
              b.textOrigin(VPos.TOP)
              b.build()
            }
            val root = new Pane()
            root.getChildren().addAll(text)
            val group = new Group(root)
            val scene = new Scene(group)
            canvas.setScene(scene, _ ⇒ latch.countDown())

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
    verify(adapter1, org.mockito.Mockito.timeout(1000).times(1)).onHostResize(278, 116)
  }

  def <>[T, S](b: Builder[_])(f: T ⇒ S) = f(b.asInstanceOf[T])

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
