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

import com.sun.glass.ui.Application
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import javafx.application.Platform
import org.digimead.digi.lib.DependencyInjection
import org.digimead.digi.lib.jfx4swt.JFX.JFX2interface
import org.digimead.lib.test.LoggingHelper
import org.eclipse.swt.widgets.Display
import org.scalatest.{ FreeSpec, Matchers }

class JFXSpec extends FreeSpec with Matchers with LoggingHelper {
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

  "JFX4SWT should start/stop event thread." in {
    thread.start()

    @volatile var flag = false
    flag should be(false)
    JFX.start(new Runnable { def run = flag = true })
    flag should be(true)
    @volatile var flagA = false
    val latchA = new CountDownLatch(1)
    Platform.runLater(new Runnable {
      def run = {
        JFX.assertEventThread()
        flagA = true
        latchA.countDown()
      }
    })
    latchA.await(1000, TimeUnit.MILLISECONDS)
    flagA should be(true)
    JFX.assertEventThread(false)

    Application.GetApplication() shouldBe a[JFXApplication]

    JFX.stop(new Runnable { def run = flag = false })
    flag should be(false)

    Display.getDefault().asyncExec(new Runnable { def run = Display.getDefault().dispose() })
  }

  override def beforeAll(configMap: org.scalatest.ConfigMap) {
    adjustLoggingBeforeAll(configMap)
    DependencyInjection(config, false)
  }
}
