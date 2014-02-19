package org

import language.implicitConversions
import language.postfixOps
import language.existentials



import collection._
import java.io.File


package object scalameter {

  type KeyValue = (Key[T], T) forSome { type T }

  private[scalameter] object dyn {
    val initialContext = new MonadicDynVar(Context.topLevel)
    val log = new MonadicDynVar[Log](Log.Console)
    val events = new MonadicDynVar[Events](Events.None)
  }

  def initialContext: Context = dyn.initialContext.value

  def log: Log = dyn.log.value

  def events: Events = dyn.events.value

  /* decorators */

  @deprecated("Use Aggregator.apply", "0.5")
  implicit def fun2ops(f: Seq[Double] => Double) = new {
    def toAggregator(n: String) = Aggregator(n)(f)
  }

  implicit final class SeqDoubleOps(val sq: Seq[Double]) extends AnyVal {
    def mean = sq.sum / sq.size

    def stdev: Double = {
      val m = mean
      var s = 0.0
      for (v <- sq) {
        val diff = v - m
        s += diff * diff
      }
      math.sqrt(s / (sq.size - 1))
    }
  }

  implicit final class SeqOps[T](val sq: Seq[T]) extends AnyVal {
    def orderedGroupBy[K](f: T => K): Map[K, Seq[T]] = {
      val map = mutable.LinkedHashMap[K, mutable.ArrayBuffer[T]]()

      for (elem <- sq) {
        val key = f(elem)
        map.get(key) match {
          case Some(b) => b += elem
          case None => map(key) = mutable.ArrayBuffer(elem)
        }
      }

      map
    }
  }

  /* misc */

  def defaultClasspath = extractClasspath(this.getClass.getClassLoader, sys.props("java.class.path"))

  def extractClasspath(classLoader: ClassLoader, default: => String): String =
    classLoader match {
      case urlclassloader: java.net.URLClassLoader => extractClasspath(urlclassloader)
      case _ =>
        val parent = classLoader.getParent
        if (parent != null)
          extractClasspath(parent, default)
        else
          default
    }

  def extractClasspath(urlclassloader: java.net.URLClassLoader): String = {
    val fileResource = "file:(.*)".r
    val files = urlclassloader.getURLs.map(_.toString) collect {
      case fileResource(file) => file
    }
    files.mkString(File.pathSeparator)
  }

  def singletonInstance[C](module: Class[C]) = module.getField("MODULE$").get(null).asInstanceOf[PerformanceTest]

}
