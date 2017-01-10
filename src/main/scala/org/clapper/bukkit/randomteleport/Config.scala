package org.clapper.bukkit.randomteleport

import org.bukkit.configuration.Configuration

private[randomteleport] class Config(private val config: Configuration) {
  val MaxCoordinate        = getInt("max", 10000)
  val MinCoordinate        = getInt("min", -10000)
  val MaxAttempts          = getInt("maxAttempts", 30)
  val TimeBetweenTeleports = getInt("timeBetweenTeleports", 120) * 1000

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
          val cfg = new Config(plugin.configuration)
          data = Some(cfg)
          cfg
        }
      }
    }
  }
}
