package org.clapper.minecraft.randomteleport

import com.joshcough.minecraft.{ScalaPlugin, CommandPlugin, ListenersPlugin}
import com.joshcough.minecraft.BukkitEnrichment._

import org.clapper.minecraft.lib.Implicits.Player._
import org.clapper.minecraft.lib.Implicits.Logging._
import org.clapper.minecraft.lib.Implicits.Block._
import org.clapper.minecraft.lib.Implicits.World._
import org.clapper.minecraft.lib.Implicits.Location._
import org.clapper.minecraft.lib.Listeners._
import org.clapper.minecraft.lib.{PermissionUtil, BlockUtil, PluginLogging, LocationUtil}

import org.bukkit.entity.Player
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.{Material, Location, World}

import scala.language.implicitConversions
import scala.collection.JavaConverters._
import scala.util.Random

import java.security.SecureRandom

private object Constants {
  val HeightDelta           = 10 // loc ok if lower than (max height - this)
  val LastTimeMetadataKey   = "last-random-teleport-time"
  val CanRandomlyTeleport   = "rp.teleport"
  val DefaultElapsedTime    = 60 * 60 * 1000 // 1 hour in milliseconds
  val TotalCoordinatesToTry = 20
}

private object Permissions {
  def canRandomlyTeleport(player: Player): Boolean = {
    true
  }
}

private case class Coordinate(x: Int, y: Int, z: Int) {
  def toLocation(world: World) = new Location(world, x, y, z)

  override def toString = s"($x, $y, $z)"
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
      teleportIfAllowed(player)
    }
  }

  override def onEnable(): Unit = {
    import scala.util.{Failure => TryFailure, Success => TrySuccess}

    super.onEnable()
    val dataDirectory = this.getDataFolder
    if (! dataDirectory.exists) {
      logMessage(s"Creating $dataDirectory")
      dataDirectory.mkdirs()
    }

    PermissionUtil.initialize(this) match {
      case TrySuccess(_) => logMessage("Initialized permissions.")
      case TryFailure(ex) => {
        logError("Failed to initialize permissions. Disabling myself.", ex)
        server.getPluginManager.disablePlugin(this)
      }
    }
  }

  private def teleportIfAllowed(player: Player) {
    if (PermissionUtil.playerHasPerm(player, "RandomTeleport.rp"))
      teleport(player)
    else
      player.sendError("You aren't permitted to use that command.")
  }

  private def teleport(player: Player) {
    val world = player.world
    val now   = System.currentTimeMillis

    val lastTeleported = lastRandomTeleportTime(player)
    val elapsed = now - lastTeleported
    logMessage(s"timeBetween=${configuration.TimeBetweenTeleports}, elapsed=${elapsed}, last=${lastTeleported}, now=${now}")
    if (elapsed < configuration.TimeBetweenTeleports) {
      val left = (configuration.TimeBetweenTeleports - elapsed)
      val leftSecs = (left / 1000) + (
        // Round up if there's ANY remainder.
        if ((left % 1000) > 0) 1 else 0
        )
      val humanLeft = if (leftSecs == 1) "a second" else s"another $leftSecs seconds"
      player.notice(s"You can't randomly teleport for $humanLeft.")
    }

    else if (randomlyTeleport(world, player)) {
      val loc = player.loc
      val sLoc = s"(${loc.x}, ${loc.y}, ${loc.z})"
      logMessage(s"Teleported ${player.name} to $sLoc")
      player.sendRawMessage(s"You have been teleported to ${sLoc}.")
      player.setMetadata(Constants.LastTimeMetadataKey,
                         RTPMetaData(now.toString, this))
    }

    else {
      logMessage(s"Failed to teleport player ${player.name}.")
      player.sendError("Sorry, we are unable to teleport you at this time.")
    }
  }

  private def randomlyTeleport(world: World, player: Player): Boolean = {
    val (min, max) = (configuration.MinCoordinate,
                      configuration.MaxCoordinate)

    // Select N random coordinates. Then, weed out the ones whose highest
    // blocks are water. If there are any left, use one of them. Otherwise,
    // randomly select one of the water ones and find a nearby safe
    // location.
    //
    // This strategy is intended to lower the probability of ending up
    // in a cave (i.e., raise the probability of landing above ground).

    val coordinates = (1 to Constants.TotalCoordinatesToTry).map { i =>
      val x = randomCoordinate(min, max)
      val z = randomCoordinate(min, max)
      val y = world.highestNonAirBlockYAt(x, z)
      Coordinate(x, y, z)
    }.
    toList

    val nonWaterCoordinates = coordinates.filter { c =>
      ! BlockUtil.isWaterType(world.blockAt(c.x, c.y, c.z).material)
    }

    logMessage(s"Total non-water coordinates: ${nonWaterCoordinates.length}")
    val coordinate = nonWaterCoordinates match {
      case Nil => {
        // No non-water locations. Take one of the locations and find
        // a safe place nearby.
        Random.shuffle(coordinates).head
      }

      case head :: Nil => {
        // Only one non-water location. Just use it. Be safe, though.
        head
      }

      case head :: rest => {
        // Many of them. Choose one at random.
        Random.shuffle(nonWaterCoordinates).head
      }
    }

    logMessage(s"Finding safe location from ${coordinate} (${world.blockAt(coordinate.x, coordinate.y, coordinate.z)})")
    val loc = LocationUtil.findSafeLocationFrom(coordinate.toLocation(world))

    player.teleport(loc)
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
