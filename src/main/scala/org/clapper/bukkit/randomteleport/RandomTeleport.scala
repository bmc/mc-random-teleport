package org.clapper.bukkit.randomteleport

import org.bukkit.entity.Player
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.{Location, Material, World}

import scala.language.implicitConversions
import scala.collection.JavaConverters._
import scala.util.Random
import java.security.SecureRandom
import java.util.logging.Level

import org.bukkit.block.Block
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.plugin.java.JavaPlugin

import org.clapper.bukkit.scalalib.Coordinate
import org.clapper.bukkit.scalalib.Implicits._

import scala.annotation.tailrec

private[randomteleport] object Constants {
  val HeightDelta           = 10 // loc ok if lower than (max height - this)
  val LastTimeMetadataKey   = "last-random-teleport-time"
  val CanRandomlyTeleport   = "org.clapper.mcRandomTeleport"
  val DefaultElapsedTime    = 60 * 60 * 1000 // 1 hour in milliseconds
  val TotalCoordinatesToTry = 20
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

final class RandomTeleportPlugin extends JavaPlugin {

  private val logger = getLogger

  private lazy val random = SecureRandom.getInstance("SHA1PRNG", "SUN")
  // Force random number generator to seed itself. See
  // https://www.cigital.com/justice-league-blog/2009/08/14/proper-use-of-javas-securerandom/
  random.nextInt()

  private lazy val config = Config(this)

  override def onDisable(): Unit = {
    super.onDisable()
  }

  override def onCommand(sender:  CommandSender,
                         command: Command,
                         alias:   String,
                         args:    Array[String]): Boolean = {
    val playerOpt: Option[Player] = sender match {
      case p: Player => Some(p)
      case _         => None
    }

    playerOpt.exists { player =>
      if (player.hasPermission(Constants.CanRandomlyTeleport)) {
        teleport(player)
        true
      }
      else {
        player.sendMessage("You are not permitted to use that command.")
        false
      }
    }
  }

  override def onEnable(): Unit = {
    super.onEnable()
    val dataDirectory = this.getDataFolder
    if (! dataDirectory.exists) {
      logger.log(Level.INFO, s"Creating $dataDirectory")
      dataDirectory.mkdirs()
    }
  }

  private def teleport(player: Player) {
    val world = player.world
    val now   = System.currentTimeMillis

    val lastTeleported = lastRandomTeleportTime(player)
    val elapsed = now - lastTeleported
    logger.log(Level.FINEST,
               s"timeBetween=${config.TimeBetweenTeleports}, " +
               s"elapsed=$elapsed, last=$lastTeleported, now=$now")
    if (elapsed < config.TimeBetweenTeleports) {
      val left = config.TimeBetweenTeleports - elapsed
      val leftSecs = (left / 1000) + (
        // Round up if there's ANY remainder.
        if ((left % 1000) > 0) 1 else 0
        )
      val humanLeft = if (leftSecs == 1) "a second" else s"another $leftSecs seconds"
      player.sendMessage(s"You can't randomly teleport for $humanLeft.")
    }

    else if (randomlyTeleport(world, player)) {
      val loc = player.getLocation
      val sLoc = s"(${loc.x.toInt}, ${loc.x.toInt}, ${loc.x.toInt})"
      logger.log(Level.FINEST, s"Teleported ${player.getName} to $sLoc")
      player.sendRawMessage(s"You have been teleported to $sLoc.")
      player.setMetadata(Constants.LastTimeMetadataKey,
                         RTPMetaData(now.toString, this))
    }

    else {
      logger.log(Level.SEVERE, s"Failed to teleport player ${player.getName}.")
      player.sendMessage("Sorry, we are unable to teleport you at this time.")
    }
  }

  private def randomlyTeleport(world: World, player: Player): Boolean = {
    val (min, max) = (config.MinCoordinate, config.MaxCoordinate)

    // Select N random coordinates. Then, weed out the ones whose highest
    // blocks are water. If there are any left, use one of them. Otherwise,
    // randomly select one of the water ones and find a nearby safe
    // location.
    //
    // This strategy is intended to lower the probability of ending up
    // in a cave (i.e., raise the probability of landing above ground).

    val coordinates = for { _ <- 1 to Constants.TotalCoordinatesToTry } yield {
      val x = randomCoordinate(min, max)
      val z = randomCoordinate(min, max)
      val y = world.highestNonAirBlockAt(x, z).y
      Coordinate(x, y, z)
    }

    val safeCoordinates = coordinates.filter { coord =>
      world.blockAt(coord).isSafe
    }

    logger.log(Level.FINEST, s"${safeCoordinates.length} non-water coordinates")

    val startingCoordinate = safeCoordinates.toList match {
      case Nil =>
        // No safe locations. Take one of the locations and find
        // a safe place nearby.
        Random.shuffle(coordinates).head

      case head :: Nil =>
        // Only one non-water location. Just use it. Be safe, though.
        head

      case head :: _ =>
        // Many of them. Choose one at random.
        Random.shuffle(safeCoordinates).head
    }

    val startingLoc = startingCoordinate.toLocation(world)
    logger.log(Level.FINEST, s"Finding safe location from $startingLoc")
    val loc = world.findSafeLocationFrom(startingLoc)

    player.teleport(loc)
  }

  private def lastRandomTeleportTime(player: Player) = {
    val now = System.currentTimeMillis
    player.getMetadata(Constants.LastTimeMetadataKey).asScala.toList match {
      case Nil                     =>  now - Constants.DefaultElapsedTime
      case (m: MetadataValue) :: _ => java.lang.Long.parseLong(m.asString)
    }
  }

  private def randomCoordinate(min: Int, max: Int): Int = {
    random.nextInt(max - min) + min
  }
}
