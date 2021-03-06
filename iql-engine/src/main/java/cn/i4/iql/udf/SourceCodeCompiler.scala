package cn.i4.iql.udf

import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.apache.spark.internal.Logging


/**
  * Created by allwefantasy on 27/8/2018.
  */
object SourceCodeCompiler extends Logging {
  private val scriptCache = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .build(
      new CacheLoader[ScriptCacheKey, AnyRef]() {
        override def load(scriptCacheKey: ScriptCacheKey): AnyRef = {
          val startTime = System.nanoTime()
          val res = compileScala(prepareScala(scriptCacheKey.code, scriptCacheKey.className))
          def timeMs: Double = (System.nanoTime() - startTime).toDouble / 1000000
          logInfo(s"generate udf time:${timeMs}")
          res
        }
      })

  def execute(scriptCacheKey: ScriptCacheKey): AnyRef = {
    scriptCache.get(scriptCacheKey)
  }

  def newInstance(clazz: Class[_]): Any = {
    val constructor = clazz.getDeclaredConstructors.head
    constructor.setAccessible(true)
    constructor.newInstance()
  }

  def getMethod(clazz: Class[_], method: String) = {
    val candidate = clazz.getDeclaredMethods.filter(_.getName == method).filterNot(_.isBridge)
    if (candidate.isEmpty) {
      throw new Exception(s"No method $method found in class ${clazz.getCanonicalName}")
    } else if (candidate.length > 1) {
      throw new Exception(s"Multiple method $method found in class ${clazz.getCanonicalName}")
    } else {
      candidate.head
    }
  }

  def compileScala(src: String): Class[_] = {
    import scala.reflect.runtime.universe
    import scala.tools.reflect.ToolBox
    val classLoader = scala.reflect.runtime.universe.getClass.getClassLoader
    val tb = universe.runtimeMirror(classLoader).mkToolBox()
    val tree = tb.parse(src)
    val clazz = tb.compile(tree).apply().asInstanceOf[Class[_]]
    clazz
  }

  def prepareScala(src: String, className: String): String = {
    src + "\n" + s"scala.reflect.classTag[$className].runtimeClass"
  }
}

class SourceCodeCompiler

case class ScriptCacheKey(code: String, className: String)

