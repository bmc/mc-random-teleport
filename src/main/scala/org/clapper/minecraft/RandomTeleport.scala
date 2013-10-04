package org.clapper.minecraft.randomteleport

import com.joshcough.minecraft.{ScalaPlugin, CommandPlugin, ListenersPlugin}
import com.joshcough.minecraft.BukkitEnrichment._

import org.clapper.minecraft.lib.Implicits.Player._
import org.clapper.minecraft.lib.Implicits.Logging._
import org.clapper.minecraft.lib.Implicits.Block._
import org.clapper.minecraft.lib.Listeners._
import org.clapper.minecraft.lib.{PluginLogging, LocationUtil}

import org.bukkit.entity.Player
import org.bukkit.block.Block
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.{Material, Location, World}

import scala.language.implicitConversions
import scala.annotation.tailrec
import scala.collection.JavaConverters._

import java.security.SecureRandom

private object Constants {
  val HeightDelta         = 10 // loc ok if lower than (max height - this)
  val LastTimeMetadataKey = "last-random-teleport-time"
  val CanRandomlyTeleport = "rp.teleport"
  val DefaultElapsedTime  = 60 * 60 * 1000 // 1 hour in milliseconds
}

private object Permissions {
  def canRandomlyTeleport(player: Player): Boolean = {
    true
  }
}

private case class RTPMetaData(name: String, plugin: Plugin) extends MetadataValue {
  def asBoolean       = false
  def asByte          = 0
  def asDouble        = 0.0
  def asFloat         = 0.0f
  def asInt           = 0
  def asLong          = 0L
  def asShort         = 0.asInstanceOf[Short]
  def asString        = name
  def getOwningPlugin = plugin
  def invalidate      = ()
  def value           = name
}

class RandomTeleportPlugin
  extends ListenersPlugin
  with    CommandPlugin
  with    PluginLogging {

  private lazy val random = SecureRandom.getInstance("SHA1PRNG", "SUN")
  // Force random number generator to see itself. See
  // https://www.cigital.com/justice-league-blog/2009/08/14/proper-use-of-javas-securerandom/
  random.nextInt()

  private lazy val configuration = ConfigData(this)

  val listeners = List(
    OnPlayerJoin { (player, event) =>
      logMessage(s"${player.name} logged in.")
    }
  )

  val command = Command("rp", "Randomly teleport in the world") { case player =>
    if (Permissions.canRandomlyTeleport(player)) {
      teleport(player)
    }
  }

  override def onEnable(): Unit = {
    super.onEnable()
    val dataDirectory = this.getDataFolder
    if (! dataDirectory.exists) {
      logMessage(s"Creating $dataDirectory")
      dataDirectory.mkdirs()
    }
  }

  private def teleport(player: Player) {
    val world = player.world
    val now   = System.currentTimeMillis

    @tailrec def successfullyTeleported(i: Int): Boolean = {
      if (i >= configuration.MaxAttempts) {
        logMessage(s"Failed to teleport ${player.name} after ${i} attempts.")
        player.sendError(s"Unable to teleport you after ${i} attempts.")
        false
      }

      else {
        val (min, max) = (configuration.MinCoordinate,
                          configuration.MaxCoordinate)
        val x     = randomCoordinate(min, max)
        val z     = randomCoordinate(min, max)
        val y     = Math.max(world.getMaxHeight, 512)
        val randomLoc = new Location(world,
                                     randomCoordinate(min, max),
                                     player.loc.y,
                                     randomCoordinate(min, max),
                                     player.loc.getYaw,
                                     player.loc.getPitch)
        val loc1 = LocationUtil.findSafeLocationFrom(randomLoc)

        val succeeded = player.teleport(loc1)
        if (! succeeded) {
          logMessage(s"Failed to teleport player ${player.name} to ($x, $z)")
          successfullyTeleported(i + 1)
        }
        else {
          player.setMetadata(Constants.LastTimeMetadataKey,
                             RTPMetaData(now.toString, this))
          true
        }
      }
    }

    val lastTeleported = lastRandomTeleportTime(player)
    val elapsed = now - lastTeleported
    logDebug(s"timeBetween=${configuration.TimeBetweenTeleports}, elapsed=${elapsed}, last=${lastTeleported}, now=${now}")
    if (elapsed < configuration.TimeBetweenTeleports) {
      val left = (configuration.TimeBetweenTeleports - elapsed)
      val leftSeconds = (left / 1000) + (
        // Round up if there's ANY remainder.
        if ((left % 1000) > 0) 1 else 0
        )
      val humanLeft = if (leftSeconds == 1) "a second" else s"another $leftSeconds seconds"
      player.notice(s"You can't teleport for $humanLeft.")
    }

    else {
      if (successfullyTeleported(0)) {
        val loc = player.loc
        val sLoc = s"(${loc.x}, ${loc.y}, ${loc.z})"
        logMessage(s"Teleported ${player.name} to $sLoc")
        player.sendRawMessage(s"You have been teleported to $sLoc")
      }
    }
  }

  private def lastRandomTeleportTime(player: Player) = {
    val now = System.currentTimeMillis
    player.getMetadata(Constants.LastTimeMetadataKey).asScala.toList match {
      case Nil                        =>  now - Constants.DefaultElapsedTime
      case (m: MetadataValue) :: tail => java.lang.Long.parseLong(m.asString)
    }
  }

  private def randomCoordinate(min: Int, max: Int): Int = {
    random.nextInt(max - min) + min
  }
}
