RandomTeleport is yet another Minecraft Bukkit server plugin that allows
users to teleport somewhere randomly within the current world.

There are other, similar, plugins already out there, including:

* [RandomPort](http://dev.bukkit.org/bukkit-plugins/randomport/)
* [Random Teleport](http://dev.bukkit.org/bukkit-plugins/random-teleport/)
* [Teleport Random Location](http://dev.bukkit.org/bukkit-plugins/teleport-random-location/)
* [Random Location Teleporter](http://dev.bukkit.org/bukkit-plugins/randomlocationteleporter/)

So, why implement yet another one? Two reasons, primarily:

* Guaranteed access to the source code. The source to this version is
  easy to find and will be available as long as the plugin is around.
* It serves as a reasonable template for other plugins.

This plugin is written in Scala and uses Josh Cough's
[Scala Bukkit plugin API](https://github.com/joshcough/MinecraftPlugins).

# Getting the Plugin

Currently, the only way to get the plugin is to build it from source. To do
that, you'll need the following:

* A Java JDK (preferably Java 7).
* A recent version of [Gradle](http://gradle.org).

## Building the Plugin

* Check out the repo.
* Run: `gradle zip`
* The resulting distribution is in `build/distributions/mc-random-teleport-VERSION.zip`,
  where `VERSION` is the current version of the plugin.

## Installing the Plugin

Unpack the zip file you built above.  Unzipping will result in an
`mc-random-teleport` subdirectory containing numerous jar files. Assuming the
top-level directory of your Minecraft world is `$WORLD`, issue the following
commands. (These commands are suitable for Unix and Mac systems. If you're on
Windows, either use the appropriate Windows commands or run the following
commands from a [Cygwin](http://www.cygwin.com/) shell.)

    $ cd mc-endless-dispenser
    $ mkdir -p $WORLD/lib
    $ cp scala-library-2.10.jar $WORLD/lib
    $ cp mclib*.jar $WORLD/lib/mclib.jar
    $ cp scala-minecraft-plugin-api*.jar RandomTeleport.jar $WORLD/plugins

Then, restart or reload your Bukkit server.

# Configuration

## Commands

The plugin provides an in-game `/rp` chat command. When invoked, the plugin
randomly chooses a location within the user's current world and teleports
the user to that world.

## Cool-down period

The avoid an excessive chunk-loading drain on the server, the plugin supports
a cool-down period. After a user uses `/rp`, he or she can't use it again for
a configurable number of seconds. (See **Configuration**, below.)

## Placement

A user will never be randomly teleported into water, lava, or air. The
plugin randomly chooses _x_ and _z_ coordinates within the current world.
It then selects the _y_ (elevation) coordinate corresponding to the topmost
non-air block at that (_x_, _z_) location.

## Configuration

Configuration is self-explanatory. When the plugin comes up, it creates
a `plugins/RandomTeleport` directory, if that directory doesn't exist. If
you want to override the default configuration, download a copy of
`src/main/resources/config.yml` from this repo, edit it, install it in that
directory, and restart or reload your server. The comments in the `config.yml`
file explain what each configuration item does.

## Plugin Permissions

The plugin currently doesn't support any permissions. It's either on or off.
If the plugin is enabled, any user can use it. Adding permissions is a future
enhancement.

# Permissions

It's not necessary to set permissions, because the plugin assumes
reasonable defaults. However, if you're using a permissions plugin such
as [Essentials Group Manager](http://wiki.ess3.net/wiki/Group_Manager),
you can control which users are permitted to change their nicknames.

The plugin supports the following permission:

- `nickname.canchange`: Set to `true` or `false`, to enable or disable
  the ability to change the nickname. Default, if not set: `true`

## Commands

Supports the following in-game chat commands:

`nickname name` sets a new nickname.
`nickname` (without arguments) shows your nickname.
`nickname -` disables your nickname.
