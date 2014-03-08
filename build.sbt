//
// Copyright (c) 2014 Alexey Aksenov ezh@ezh.msk.ru
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// DEVELOPMENT CONFIGURATION

import sbt.application._
import sbt.osgi.manager._

OSGiManager ++ Application // ++ sbt.scct.ScctPlugin.instrumentSettings - ScctPlugin is broken, have no time to fix

name := "digi-lib-jfx4swt-7"

description := "JFX4SWT - JavaFX library adapter for SWT framework."

licenses := Seq("GNU Lesser General Public License, Version 3.0" -> url("http://www.gnu.org/licenses/lgpl-3.0.txt"))

organization := "org.digimead"

organizationHomepage := Some(url("http://digimead.org"))

homepage := Some(url("https://github.com/ezh/digi-lib-jfx4swt-7"))

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

inConfig(OSGiConf)({
  import OSGiKey._
  Seq[Project.Setting[_]](
        osgiBndBundleSymbolicName := "org.digimead.lib.swt4jfx",
        osgiBndBundleCopyright := "Copyright © 2014 Alexey B. Aksenov/Ezh. All rights reserved.",
        osgiBndBundleLicense := "http://www.gnu.org/licenses/lgpl-3.0.txt;description=GNU Lesser General Public License, Version 3.0",
        osgiBndImportPackage := List(
          "com.escalatesoft.subcut.inject",
          "org.digimead.digi.lib",
          "org.digimead.digi.lib.api",
          "org.digimead.digi.lib.log.api",
          "org.eclipse.swt.events",
          "org.eclipse.swt.graphics",
          "org.eclipse.swt.widgets",
          "scala",
          "scala.collection",
          "scala.collection.generic",
          "scala.collection.immutable",
          "scala.collection.mutable",
          "scala.collection.script",
          "scala.concurrent",
          "scala.ref",
          "scala.reflect",
          "scala.runtime",
          "scala.util",
          "javafx.application",
          "com.sun.browser.plugin",
          "com.sun.deploy.uitoolkit.impl.fx",
          "com.sun.deploy.uitoolkit.impl.fx.ui",
          "com.sun.deploy.uitoolkit.impl.fx.ui.resources",
          "com.sun.deploy.uitoolkit.impl.fx.ui.resources.image",
          "com.sun.glass.events",
          "com.sun.glass.ui",
          "com.sun.glass.ui.delegate",
          "com.sun.glass.ui.gtk",
          "com.sun.glass.ui.mac",
          "com.sun.glass.ui.win",
          "com.sun.glass.ui.x11",
          "com.sun.glass.utils",
          "com.sun.javafx",
          "com.sun.javafx.animation",
          "com.sun.javafx.animation.transition",
          "com.sun.javafx.applet",
          "com.sun.javafx.application",
          "com.sun.javafx.beans",
          "com.sun.javafx.beans.annotations",
          "com.sun.javafx.beans.event",
          "com.sun.javafx.binding",
          "com.sun.javafx.charts",
          "com.sun.javafx.collections",
          "com.sun.javafx.collections.annotations",
          "com.sun.javafx.collections.transformation",
          "com.sun.javafx.css",
          "com.sun.javafx.css.converters",
          "com.sun.javafx.css.parser",
          "com.sun.javafx.cursor",
          "com.sun.javafx.effect",
          "com.sun.javafx.embed",
          "com.sun.javafx.event",
          "com.sun.javafx.font",
          "com.sun.javafx.fxml",
          "com.sun.javafx.fxml.builder",
          "com.sun.javafx.fxml.expression",
          "com.sun.javafx.geom",
          "com.sun.javafx.geom.transform",
          "com.sun.javafx.iio",
          "com.sun.javafx.iio.bmp",
          "com.sun.javafx.iio.common",
          "com.sun.javafx.iio.gif",
          "com.sun.javafx.iio.jpeg",
          "com.sun.javafx.iio.png",
          "com.sun.javafx.image",
          "com.sun.javafx.image.impl",
          "com.sun.javafx.jmx",
          "com.sun.javafx.logging",
          "com.sun.javafx.menu",
          "com.sun.javafx.perf",
          "com.sun.javafx.property",
          "com.sun.javafx.property.adapter",
          "com.sun.javafx.robot",
          "com.sun.javafx.robot.impl",
          "com.sun.javafx.runtime",
          "com.sun.javafx.runtime.async",
          "com.sun.javafx.runtime.eula",
          "com.sun.javafx.scene",
          "com.sun.javafx.scene.control",
          "com.sun.javafx.scene.control.behavior",
          "com.sun.javafx.scene.control.skin",
          "com.sun.javafx.scene.control.skin.caspian",
          "com.sun.javafx.scene.control.skin.resources",
          "com.sun.javafx.scene.input",
          "com.sun.javafx.scene.layout.region",
          "com.sun.javafx.scene.paint",
          "com.sun.javafx.scene.shape",
          "com.sun.javafx.scene.text",
          "com.sun.javafx.scene.transform",
          "com.sun.javafx.scene.traversal",
          "com.sun.javafx.scene.web",
          "com.sun.javafx.scene.web.behavior",
          "com.sun.javafx.scene.web.skin",
          "com.sun.javafx.sg",
          "com.sun.javafx.sg.prism",
          "com.sun.javafx.stage",
          "com.sun.javafx.tk",
          "com.sun.javafx.tk.desktop",
          "com.sun.javafx.tk.quantum",
          "com.sun.javafx.util",
          "com.sun.media.jfxmedia",
          "com.sun.media.jfxmedia.control",
          "com.sun.media.jfxmedia.effects",
          "com.sun.media.jfxmedia.events",
          "com.sun.media.jfxmedia.locator",
          "com.sun.media.jfxmedia.logging",
          "com.sun.media.jfxmedia.track",
          "com.sun.media.jfxmediaimpl",
          "com.sun.media.jfxmediaimpl.platform",
          "com.sun.media.jfxmediaimpl.platform.gstreamer",
          "com.sun.media.jfxmediaimpl.platform.java",
          "com.sun.media.jfxmediaimpl.platform.osx",
          "com.sun.openpisces",
          "com.sun.prism",
          "com.sun.prism.camera",
          "com.sun.prism.d3d",
          "com.sun.prism.d3d.hlsl",
          "com.sun.prism.image",
          "com.sun.prism.impl",
          "com.sun.prism.impl.packrect",
          "com.sun.prism.impl.paint",
          "com.sun.prism.impl.ps",
          "com.sun.prism.impl.shape",
          "com.sun.prism.j2d",
          "com.sun.prism.j2d.paint",
          "com.sun.prism.paint",
          "com.sun.prism.ps",
          "com.sun.prism.render",
          "com.sun.prism.shader",
          "com.sun.prism.shape",
          "com.sun.prism.tkal",
          "com.sun.prism.util.tess",
          "com.sun.prism.util.tess.impl.tess",
          "com.sun.scenario",
          "com.sun.scenario.animation",
          "com.sun.scenario.animation.shared",
          "com.sun.scenario.effect",
          "com.sun.scenario.effect.impl",
          "com.sun.scenario.effect.impl.hw",
          "com.sun.scenario.effect.impl.hw.d3d",
          "com.sun.scenario.effect.impl.hw.d3d.hlsl",
          "com.sun.scenario.effect.impl.prism",
          "com.sun.scenario.effect.impl.prism.ps",
          "com.sun.scenario.effect.impl.prism.sw",
          "com.sun.scenario.effect.impl.state",
          "com.sun.scenario.effect.impl.sw",
          "com.sun.scenario.effect.impl.sw.java",
          "com.sun.scenario.effect.impl.sw.sse",
          "com.sun.scenario.effect.light",
          "com.sun.t2k",
          "com.sun.webpane.perf",
          "com.sun.webpane.platform",
          "com.sun.webpane.platform.event",
          "com.sun.webpane.platform.graphics",
          "com.sun.webpane.sg",
          "com.sun.webpane.sg.prism",
          "com.sun.webpane.sg.prism.resources",
          "com.sun.webpane.sg.prism.theme",
          "com.sun.webpane.sg.theme",
          "com.sun.webpane.webkit",
          "com.sun.webpane.webkit.dom",
          "com.sun.webpane.webkit.network",
          "com.sun.webpane.webkit.network.about",
          "com.sun.webpane.webkit.network.data",
          "com.sun.webpane.webkit.unicode",
          "javafx.animation",
          "javafx.beans",
          "javafx.beans.binding",
          "javafx.beans.property",
          "javafx.beans.property.adapter",
          "javafx.beans.value",
          "javafx.collections",
          "javafx.concurrent",
          "javafx.embed.swing",
          "javafx.embed.swt",
          "javafx.event",
          "javafx.fxml",
          "javafx.geometry",
          "javafx.scene",
          "javafx.scene.canvas",
          "javafx.scene.chart",
          "javafx.scene.control",
          "javafx.scene.control.cell",
          "javafx.scene.effect",
          "javafx.scene.image",
          "javafx.scene.input",
          "javafx.scene.layout",
          "javafx.scene.media",
          "javafx.scene.paint",
          "javafx.scene.shape",
          "javafx.scene.text",
          "javafx.scene.transform",
          "javafx.scene.web",
          "javafx.stage",
          "javafx.util",
          "javafx.util.converter",
          "netscape.javascript"),
        osgiBndExportPackage := List("org.digimead.digi.lib.*"),
        osgiBndPrivatePackage := List())
})

crossScalaVersions := Seq("2.10.3")

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-Xcheckinit", "-feature", "-Xfatal-warnings", "-Xelide-below", "ALL")

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.7", "-target", "1.7")

javacOptions in doc := Seq("-source", "1.7")

if (sys.env.contains("XBOOTCLASSPATH")) Seq(javacOptions += "-Xbootclasspath:" + sys.env("XBOOTCLASSPATH")) else Seq()

//
// Custom local options
//

resolvers += "swt-repo" at "https://swt-repo.googlecode.com/svn/repo/"

ivyConfigurations += config("compileonly").hide

unmanagedClasspath in Compile ++= update.value.select(configurationFilter("compileonly"))

resolvers += "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/"

libraryDependencies ++= {
  val os = (sys.props("os.name"), sys.props("os.arch")) match {
    case ("Linux", "amd64") => "gtk.linux.x86_64"
    case ("Linux", _) => "gtk.linux.x86"
    case ("Mac OS X", "amd64" | "x86_64") => "cocoa.macosx.x86_64"
    case ("Mac OS X", _) => "cocoa.macosx.x86"
    case (os, "amd64") if os.startsWith("Windows") => "win32.win32.x86_64"
    case (os, _) if os.startsWith("Windows") => "win32.win32.x86"
    case (os, arch) => sys.error("Cannot obtain lib for OS '" + os + "' and architecture '" + arch + "'")
  }
  val artifact = "org.eclipse.swt." + os
  Seq(
    "org.eclipse.swt" % artifact % "4.2.1" % "compileonly",
    "org.eclipse.swt" % artifact % "4.2.1" % "test",
    "org.digimead" %% "digi-lib" % "0.2.3.4-SNAPSHOT",
    "org.digimead" %% "digi-lib-test" % "0.2.2.4-SNAPSHOT" % "test"
  )
}

//
// Testing
//

parallelExecution in Test := false

testGrouping in Test <<= (definedTests in Test) map { tests =>
  tests map { test =>
    new Tests.Group(
      name = test.name,
      tests = Seq(test),
      runPolicy = Tests.SubProcess(javaOptions = Seq.empty[String]))
  }
}

//logLevel := Level.Debug
