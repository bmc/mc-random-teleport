package org.clapper.minecraft.randomteleport

import java.util.logging.Logger
import org.bukkit.configuration.Configuration

private[randomteleport] class ConfigData(private val config: Configuration,
                                         private val logger: Logger) {
  val MaxCoordinate        = getInt("max", 10000)
  val MinCoordinate        = getInt("min", -10000)
  val MaxAttempts          = getInt("maxAttempts", 30)
  val TimeBetweenTeleports = getInt("timeBetweenTeleports", 120) * 1000

  private def getInt(key: String, default: Int): Int = {
    val res = config.getInt(key)
    if (res > 0) res else default
  }
}

private[randomteleport] object ConfigData {
  private var data: Option[ConfigData] = None

  /** Get the configuration.
    */
  def apply(plugin: RandomTeleportPlugin): ConfigData = {
    this.synchronized {
      data match {
        case Some(cfg) => cfg

        case None => {
          plugin.saveDefaultConfig()
          val cfg = new ConfigData(plugin.getConfig, plugin.logger)
          data = Some(cfg)
          cfg
        }
      }
    }
  }
}
