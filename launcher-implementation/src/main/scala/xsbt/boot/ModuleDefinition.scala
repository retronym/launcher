package xsbt.boot

import Pre._
import java.io.{ File, PrintWriter }
import java.net.URLClassLoader

final class ModuleDefinition(val configuration: UpdateConfiguration, val extraClasspath: Array[File], val target: UpdateTarget, val failLabel: String) {
  def retrieveFailed: Nothing = fail("")
  def retrieveCorrupt(missing: Iterable[(String, Throwable)]): Nothing = {
    val writer = new java.io.StringWriter()
    val printWriter = new PrintWriter(writer)
    try {
      printWriter.write("Unable to load some classes. Possible corrupt JARs.")
      for ((className, throwable) <- missing) {
        printWriter.append("missing: ").append(className).println()
        printWriter.append("reason: ")
        throwable.printStackTrace(printWriter)
        printWriter.println()
      }
    } finally {
      printWriter.close()
    }
    fail(writer.toString)
  }
  private def fail(extra: String) =
    throw new xsbti.RetrieveException(versionString, "could not retrieve " + failLabel + extra)
  private def versionString: String = target match { case _: UpdateScala => configuration.getScalaVersion; case a: UpdateApp => Value.get(a.id.version) }
}

final class RetrievedModule(val fresh: Boolean, val definition: ModuleDefinition, val detectedScalaVersion: Option[String], val resolvedAppVersion: Option[String], val baseDirectories: List[File]) {
  /** Use this constructor only when the module exists already, or when its version is not dynamic (so its resolved version would be the same) */
  def this(fresh: Boolean, definition: ModuleDefinition, detectedScalaVersion: Option[String], baseDirectories: List[File]) =
    this(fresh, definition, detectedScalaVersion, None, baseDirectories)

  lazy val monkeys: Array[File] = sys.props.get("sbt.launcher.cp.prepend").toArray.flatMap(ms => ms.split(",").map(new File(_)))
  lazy val classpath: Array[File] = getJars(baseDirectories)
  lazy val fullClasspath: Array[File] = concat(concat(monkeys, classpath), definition.extraClasspath)

  def createLoader(parentLoader: ClassLoader): ClassLoader =
    new URLClassLoader(toURLs(fullClasspath), parentLoader)
}
