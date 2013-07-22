package org.clapper.minecraft.nickname

import jcdc.pluginfactory.{ScalaPlugin, CommandPlugin, ListenerPlugin, ListenersPlugin}
import jcdc.pluginfactory.Listeners._

import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.event.player.{PlayerJoinEvent, PlayerMoveEvent}
import org.bukkit.entity.Player
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.Plugin

import scala.language.implicitConversions
import scala.collection.JavaConverters._

private class EnrichedPlayer(val player: Player) {
  def notice(message: String) = {
    player.sendRawMessage(s"§e${message}")
  }
}

private object Implicits {
  implicit def playerToEnrichedPlayer(p: Player) = new EnrichedPlayer(p)
  implicit def enrichedPlayerToPlayer(e: EnrichedPlayer) = e.player
}

trait NicknameConstants {
  val CAN_CHANGE_PERM = "nickname.canchange"
  val METADATA_KEY    = "nickname"
}

trait NicknamePermissions extends NicknameConstants {
  self: ScalaPlugin =>

  def permittedToChangeName(player: Player) = {
    (! player.isPermissionSet(CAN_CHANGE_PERM)) ||
    player.hasPermission(CAN_CHANGE_PERM)
  }
}

case class NicknameMetadata(name: String, plugin: Plugin) extends MetadataValue {
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

class NicknamePlugin
  extends ListenersPlugin
  with    CommandPlugin
  with    NicknamePermissions {

  import Implicits._

  private lazy val logger = getLogger()

  val listeners = List(
    OnPlayerJoin { (player, event) =>
      logMessage(s"${player.name} logged in.")
      player.getMetadata(METADATA_KEY).asScala.toList match {
        case Nil =>
        logMessage(s"${player.name} has no saved nickname.")

        case meta :: whatever =>
          val name = meta.asInstanceOf[MetadataValue].asString
          logMessage(s"${player.name} has saved nickname: $name")
          setName(player, Some(name))
      }
    }
  )

  val command = Command("nk", "Change or show your nickname.", slurp) {
    case (player, name) => {
      val trimmedName = name.trim

      if (! permittedToChangeName(player)) {
        logMessage(s"Player ${player.name} isn't permitted to change nicknames.")
        player.sendError("You aren't permitted to change your nickname.")
      }

      else if (trimmedName == "-") {
        setName(player, None)
        player.notice("Your nickname has been cleared.")
      }

      else if (trimmedName == "") {
        val currentName = getName(player)
        player.notice(s"Your current nickname is: $currentName")
      }

      else {
        setName(player, Some(trimmedName))
        player.notice(s"Your nickname is now: $trimmedName")
      }
    }
  }

  override def onEnable(): Unit = {
    super.onEnable()
    logMessage("I'm alive! Foo")
  }

  private def setName(player: Player, nameOpt: Option[String]): Unit = {
    nameOpt match {
      case None =>
        player.setDisplayName(null)
        player.setPlayerListName(null)
        player.removeMetadata(METADATA_KEY, this)

      case Some(name) =>
        player.setDisplayName(name)
        player.setPlayerListName(name)
        player.setMetadata(METADATA_KEY, NicknameMetadata(name, this))
    }
  }

  private def getName(player: Player) = player.getDisplayName

  private def logMessage(msg: String) = {
    logger.info(s"[$name] $msg")
  }
}
