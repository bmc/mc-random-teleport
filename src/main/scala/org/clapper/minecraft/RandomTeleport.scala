package org.clapper.minecraft.randomteleport

import com.joshcough.minecraft.{ScalaPlugin, CommandPlugin, ListenersPlugin}

import org.clapper.minecraft.lib.Implicits.Player._
import org.clapper.minecraft.lib.Implicits.Logging._
import org.clapper.minecraft.lib.Implicits.Block._
import org.clapper.minecraft.lib.Listeners._
import org.clapper.minecraft.lib.{PluginLogging, ScalaPluginExtras}

import org.bukkit.entity.Player
import org.bukkit.block.Block
import org.bukkit.World
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.{Material, Location}

import scala.language.implicitConversions
import scala.annotation.tailrec
import scala.collection.JavaConverters._

import java.security.SecureRandom

private object Constants {
  val HeightDelta         = 10 // location ok if lower than (max height - this)
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
        val world = player.world
        val block = highestNonAirBlock(world, x, z)
        val loc   = block.loc
        val y     = loc.y.toInt
        logDebug(s"Highest block at ${block.loc.x}, ${block.loc.z} is $block")

        val succeeded = if (landingPointOkay(block, world.getMaxHeight - 10)) {
          // Teleport one above the location.
          val newLoc = new Location(world,
                                    loc.x.toDouble,
                                    (loc.y + 1).toDouble,
                                    loc.z.toDouble)
          val ok = player.teleport(newLoc)
          if (! ok)
            logMessage("Failed to teleport player ${player.name} to ($x, $z)")
          else
            player.setMetadata(Constants.LastTimeMetadataKey,
                               RTPMetaData(now.toString, this))
          ok
        }

        else {
          logDebug { s"Can't teleport player to (x=$x, y=$y, z=$z): " +
                     s"Block ${block} isn't a suitable location." }
          false
        }

        if (! succeeded)
          successfullyTeleported(i + 1)
        else
          succeeded
      }
    }

    val lastTeleported = lastRandomTeleportTime(player)
    val elapsed = now - lastTeleported
    logDebug(s"timeBetween=${configuration.TimeBetweenTeleports}, elapsed=${elapsed}, last=${lastTeleported}, now=${now}")
    if (elapsed < configuration.TimeBetweenTeleports) {
      val left = (configuration.TimeBetweenTeleports - elapsed) / 1000
      val sec  = if (left == 1) "second" else "seconds"
      player.notice(s"You can't teleport for another $left ${sec}.")
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

  private def highestNonAirBlock(world: World, x: Int, z: Int): Block = {

    @tailrec def findNonAirBlock(y: Int): Block = {
      val block = world.blockAt(x, y, z)
      if (block.getType != Material.AIR)
        block
      else
        findNonAirBlock(y - 1)
    }

    findNonAirBlock(world.getHighestBlockYAt(x, z))
  }

  private def landingPointOkay(block: Block, maxElevation: Int): Boolean = {
    (block.loc.y.toInt < maxElevation) && (blockOkay(block))
  }

  private def blockOkay(block: Block): Boolean = {
    (block.getType != Material.WATER) &&
    (block.getType != Material.STATIONARY_WATER) &&
    (block.getType != Material.LAVA) &&
    (block.getType != Material.STATIONARY_LAVA) &&
    (block.getType != Material.FIRE)
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
