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
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.chart.{ NumberAxis, StackedAreaChart, XYChart }
import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.jfx4swt.JFX.JFX2interface
import org.digimead.lib.test.LoggingHelper
import org.eclipse.swt.widgets.Display
import org.mockito.Matchers.anyBoolean
import org.mockito.Mockito.{ spy, timeout, verify }
import org.scalatest.{ FreeSpec, Matchers }

class JFXTest extends FreeSpec with Matchers with LoggingHelper {
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

  "Test StackedAreaChart" in {
    val adapter = JFX.execNGet {
      val chart = createChart()
      chart.setAnimated(false)
      val scene = new Scene(chart, 800, 600)
      val adapter = spy(new TestAdapter)
      val host = new FXHost(adapter)
      val stage = new EmbeddedWindow(host)
      stage.show()
      stage.setScene(scene)
      adapter
    }
    verify(adapter, timeout(1000).times(1)).redraw()
    verify(adapter, timeout(100).times(1)).setPreferredSize(800, 600)
    verify(adapter, timeout(100).times(0)).requestFocus()
    verify(adapter, timeout(100).times(0)).setEnabled(anyBoolean)
    verify(adapter, timeout(100).times(0)).traverseFocusOut(anyBoolean)
    adapter.frameOne.get().length should be(800 * 600 * 4)
    adapter.frameTwo.get().length should be(800 * 600 * 4)
    adapter.frameEmpty.get() should be(null)
    adapter.frameFull.get() should be(adapter.frameOne.get())
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
