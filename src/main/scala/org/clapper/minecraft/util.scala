package org.clapper.minecraft.randomteleport

import org.bukkit.{Material, Location, World}
import com.joshcough.minecraft.BukkitEnrichment._
import org.clapper.minecraft.lib.Logging

import scala.annotation.tailrec
import java.util.logging.Logger

// Adapted from Bukkit Essentials
class LocationUtil(val logger: Logger) extends Logging {

  private object LocationConstants {
    val HollowMaterials = Set(
      Material.AIR,
      Material.SAPLING,
      Material.POWERED_RAIL,
      Material.DETECTOR_RAIL,
      Material.LONG_GRASS,
      Material.DEAD_BUSH,
      Material.YELLOW_FLOWER,
      Material.RED_ROSE,
      Material.BROWN_MUSHROOM,
      Material.RED_MUSHROOM,
      Material.TORCH,
      Material.REDSTONE_WIRE,
      Material.SEEDS,
      Material.SIGN_POST,
      Material.WOODEN_DOOR,
      Material.LADDER,
      Material.RAILS,
      Material.WALL_SIGN,
      Material.LEVER,
      Material.STONE_PLATE,
      Material.IRON_DOOR_BLOCK,
      Material.WOOD_PLATE,
      Material.REDSTONE_TORCH_OFF,
      Material.REDSTONE_TORCH_ON,
      Material.STONE_BUTTON,
      Material.SNOW,
      Material.SUGAR_CANE_BLOCK,
      Material.DIODE_BLOCK_OFF,
      Material.DIODE_BLOCK_ON,
      Material.PUMPKIN_STEM,
      Material.MELON_STEM,
      Material.VINE,
      Material.FENCE_GATE,
      Material.WATER_LILY,
      Material.NETHER_WARTS,
      Material.CARPET
    )
  }

  private val Radius = 3
  private case class Coordinate(x: Int, y: Int, z: Int)
  private val Radii = (for {x <- (-Radius to Radius)
                            y <- (-Radius to Radius)
                            z <- (-Radius to Radius)}
                         yield Coordinate(x, y, z)).toList

  def findSafeLocationFrom(loc: Location): Location = {
    val world = loc.getWorld;
    val x = loc.getBlockX
    val z = loc.getBlockZ
    val y = Math.round(loc.getY).toInt

    val y2 = findGroundedBlock(world, x, y, z)
    val (x2, z2) = if (blockIsUnsafe(world, x, y2, z)) {
      val newX = if (x == Math.round(loc.getX)) x - 1 else x + 1
      val newZ = if (z == Math.round(loc.getZ)) z - 1 else z + 1
      (newX, newZ)
    }
    else {
      (x, z)
    }

    val (x3, y3, z3) = lookNearby(world, x2, y2, z2)
    val (x4, y4, z4) = lookUppish(world, x3, y3, z3)
    val (x5, y5, z5) = lookDownish(world, x4, y4, z4)

    new Location(world, x5 + 0.5, y5, z5 + 0.5, loc.getYaw(), loc.getPitch())
  }

  private def lookUppish(world: World, x: Int, y: Int, z: Int): (Int, Int, Int) = {
    @tailrec def doFind(x: Int, y: Int, z: Int): (Int, Int, Int) = {
      if (! blockIsUnsafe(world, x, y, z))
        (x, y, z)

      else {
        val y2 = y + 1
        if (y2 >= world.getMaxHeight)
          (x + 1, y2, z)
        else
          doFind(x, y2, z)
      }
    }

    doFind(x, y, z)
  }

  private def lookDownish(world: World, x: Int, y: Int, z: Int): (Int, Int, Int) = {

    @tailrec def doFind(x: Int, y: Int, z: Int): (Int, Int, Int) = {
      if (! blockIsUnsafe(world, x, y, z))
        (x, y, z)

      else {
        val (newX, newY) = if (y > 2)
          (x, y - 1)
        else {
          (x + 1, world.getHighestBlockYAt(x + 1, z))
        }

        doFind(newX, newY, z)
      }
    }

    doFind(x, y, z)
  }

  private def lookNearby(world: World, x: Int, y: Int, z: Int): (Int, Int, Int) = {
    val origX = x
    val origY = y
    val origZ = z

    @tailrec def doFind(x: Int, y: Int, z: Int, coordinates: List[Coordinate]):
      (Int, Int, Int) = {

      if (! blockIsUnsafe(world, x, y, z))
        (x, y, z)

      else {
        coordinates match {
          case Nil       => (x, y + Radius, z)
          case c :: tail => doFind(origX + c.x, origY + c.y, origZ + c.z, tail)
        }
      }
    }

    doFind(x, y, z, Radii)
  }

  // Find a block that isn't floating in the air. Returns new Y.
  private def findGroundedBlock(world: World, x: Int, y: Int, z: Int): Int = {
    val origY = y

    // Locate next block (downward) that isn't floating on air. Returns new Y.
    @tailrec def doFind(x: Int, y: Int, z: Int): Int = {
      if (! blockIsAboveAir(world, x, y, z))
        y
      else if (y < 1)
        origY
      else
        doFind(x, y - 1, z)
    }

    doFind(x, y, z)
  }

  private def blockIsUnsafe(world: World, x: Int, y: Int, z: Int): Boolean = {
    blockIsDamaging(world, x, y, z) || blockIsAboveAir(world, x, y, z)
  }

  private def blockIsAboveAir(world: World, x: Int, y: Int, z: Int): Boolean = {
    val below = world.getBlockAt(x, y - 1, z)
    LocationConstants.HollowMaterials.contains(below.getType)
  }

  private def blockIsDamaging(world: World, x: Int, y: Int, z: Int): Boolean = {
    val below = world.getBlockAt(x, y - 1, z)
    val blockType = below.getType
    val Const = LocationConstants

    (blockType == Material.LAVA) ||
    (blockType == Material.STATIONARY_LAVA) ||
    (blockType == Material.FIRE) ||
    (blockType == Material.BED_BLOCK) ||
    (blockType == Material.WATER) ||
    (blockType == Material.STATIONARY_WATER) ||
    (! Const.HollowMaterials.contains(world.getBlockAt(x, y, z).getType)) ||
    (! Const.HollowMaterials.contains(world.getBlockAt(x, y + 1, z).getType))
  }
}
