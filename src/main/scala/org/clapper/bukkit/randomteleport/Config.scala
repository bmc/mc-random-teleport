package org.clapper.bukkit.randomteleport

import org.bukkit.configuration.Configuration
import org.clapper.bukkit.scalalib.ScalaLogger
import org.clapper.bukkit.scalalib.Implicits._

import scala.concurrent.duration._

private[randomteleport] class Config(private val config: Configuration,
                                     private val log:    ScalaLogger) {
  val maxCoordinate        = getInt("max", 10000)
  val minCoordinate        = getInt("min", -10000)
  val maxAttempts          = getInt("maxAttempts", 30)

  val DefaultDelay         = 3.seconds

  val teleportDelay: Duration = config
    .getDuration("teleportDelay", DefaultDelay)
    .recover {
      case e: Exception =>
        log.error(e.getMessage)
        log.error(s"Assuming default of $DefaultDelay")
        DefaultDelay
    }
    .get

  // Milliseconds.
  val timeBetweenTeleports: Long = config
    .getDuration("timeBetweenTeleports", DefaultDelay)
    .recover {
      case e: Exception =>
        log.error(e.getMessage)
        log.error(s"Assuming default of $DefaultDelay")
        DefaultDelay
    }
    .get
    .toMillis

  private def getInt(key: String, default: Int): Int = {
    val res = config.getInt(key)
    if (res > 0) res else default
  }
}

private[randomteleport] object Config {
  private var data: Option[Config] = None

  /** Get the configuration.
    */
  def apply(plugin: RandomTeleportPlugin): Config = {
    this.synchronized {
      data match {
        case Some(cfg) => cfg

        case None => {
          plugin.saveDefaultConfig()
          val cfg = new Config(plugin.configuration, plugin.logger)
          data = Some(cfg)
          cfg
        }
      }
    }
  }
}
