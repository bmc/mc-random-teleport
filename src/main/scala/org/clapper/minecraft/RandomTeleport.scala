package org.clapper.minecraft.randomteleport

import jcdc.pluginfactory.{ScalaPlugin, CommandPlugin, ListenersPlugin}

import org.bukkit.entity.Player
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.Location

import scala.language.implicitConversions
import scala.annotation.tailrec
import scala.collection.JavaConverters._

import java.security.SecureRandom

private class EnrichedPlayer(val player: Player) {
  def notice(message: String) = {
    player.sendRawMessage(s"§e${message}")
  }
}

private object Implicits {
  implicit def playerToEnrichedPlayer(p: Player) = new EnrichedPlayer(p)
  implicit def enrichedPlayerToPlayer(e: EnrichedPlayer) = e.player
}

trait RandomTeleportConstants {
  val MIN_X               = -10000
  val MIN_Z               = -10000
  val MAX_X               = 10000
  val MAX_Z               = 10000
  val HEIGHT_DELTA        = 10 // location ok if lower than (max height - this)
  val MAX_ATTEMPTS        = 10
  val METADATA_KEY        = "last-random-teleport-time"
  val MIN_TIME_DELTA      = 120 * 1000 // 2 minutes, as ms
  val CAN_RANDOM_TELEPORT = "rp.teleport"
}

trait Logging {
  self: ScalaPlugin =>

  private lazy val logger = getLogger()

  def logMessage(msg: String) = {
    // logger.info(s"[$name] $msg")
    logger.info(s"$msg")
  }
}

trait RandomTeleportPermissions extends Logging with RandomTeleportConstants {
  self: ScalaPlugin =>

  def ifPermittedToRandomTeleport(player: Player)(code: => Unit) = {
    if (! player.isPermissionSet(CAN_RANDOM_TELEPORT)) {
      code
    }

    else {
      logMessage(s"Player ${player.name} isn't permitted to random-teleport.")
      player.sendError("You aren't permitted teleport randomly.")
    }
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
  with    Logging
  with    RandomTeleportConstants
  with    RandomTeleportPermissions {

  private lazy val random = new SecureRandom()

  import Implicits._

  val listeners = List(
    OnPlayerJoin { (player, event) =>
      logMessage(s"${player.name} logged in.")
    }
  )

  val command = Command("rp", "Randomly teleport in the world") { case player =>
    ifPermittedToRandomTeleport(player) {
      teleport(player)
    }
  }

  private def teleport(player: Player) {
    val world          = player.world
    val maxWorldHeight = world.getMaxHeight
    val maxElevation   = maxWorldHeight - 10
    val now            = System.currentTimeMillis

    @tailrec def successfullyTeleported(i: Int): Boolean = {
      if (i > MAX_ATTEMPTS) {
        logMessage(s"Failed to teleport ${player.name} after ${i} attempts.")
        player.sendError(s"Unable to teleport you after ${i} attempts.")
        false
      }

      else {
        val x = randomCoordinate(MIN_X, MAX_X)
        val z = randomCoordinate(MIN_Z, MAX_Z)
        val world = player.world
        val y = world.getHighestBlockYAt(x, z)
        val succeeded = if (y < maxElevation) {
          val loc = new Location(world, x.toDouble, y.toDouble, z.toDouble)
          val ok = player.teleport(loc)
          if (! ok)
            logMessage("Failed to teleport player ${player.name} to ($x, $z)")
          else
            player.setMetadata(METADATA_KEY,
                               RTPMetaData(now.toString, this))
          ok
        }
        else {
          logMessage("Can't teleport player to ($x, $y): Too high.")
          false
        }

        if (! succeeded)
          successfullyTeleported(i + 1)
        else
          succeeded
      }
    }

    val elapsed = now - lastRandomTeleportTime(player)
    if (elapsed < MIN_TIME_DELTA) {
      val left = MIN_TIME_DELTA - elapsed
      val sec  = if (left == 1) "second" else "seconds"
      player.notice("You can't teleport for another $left ${sec}.")
    }

    else {
      successfullyTeleported(0)
    }
  }

  private def lastRandomTeleportTime(player: Player) = {
    player.getMetadata(METADATA_KEY).asScala.toList match {
      case Nil       => System.currentTimeMillis
      case m :: tail => java.lang.Long.parseLong(m.asInstanceOf[MetadataValue].asString)
    }
  }

  private def randomCoordinate(min: Int, max: Int): Int = {
    random.nextInt(max - min) + min
  }

  override def onEnable(): Unit = {
    super.onEnable()
    logMessage("I'm alive!")
  }
}
