# RandomTeleport

RandomTeleport is yet another Minecraft Bukkit server plugin that allows
users to teleport somewhere randomly within the current world.

There are other, similar, plugins already out there, including:

* [RandomPort](http://dev.bukkit.org/bukkit-plugins/randomport/)
* [Random Teleport](http://dev.bukkit.org/bukkit-plugins/random-teleport/)
* [Teleport Random Location](http://dev.bukkit.org/bukkit-plugins/teleport-random-loc/)
* [Random Location Teleporter](http://dev.bukkit.org/bukkit-plugins/randomlocationteleporter/)

So, why implement yet another one? Several reasons:

* I don't like the apparent lack of real randomness in the other plugins.
  (They revisit the same areas too often.) This plugin attempts to be
  more random in its choices of locations.
* This version takes a bit of extra care to try to avoid teleporting the
  player inside a wall.
* Guaranteed access to the source code. The source to this version is
  easy to find and will be available as long as the plugin is around.
* It serves as a reasonable template for other plugins.

This plugin is written in Scala and uses Josh Cough's
[Scala Bukkit plugin API](https://github.com/joshcough/MinecraftPlugins).

## In-game Usage

The plugin provides an in-game `/rp` chat command. When  you type `/rp`, the plugin
randomly chooses a location within your current world and teleports you to that
location.

You will never be randomly teleported into water, lava, or air. The plugin takes
great pains not to teleport you inside a solid block. However, it's entirely
possible that you will be teleported to a location underground. It's a good idea
to have torches ready, since you might be teleported to a place that has
no ambient light.

## Administration

### Configuration

#### Cool-down period

The avoid an excessive chunk-loading drain on the server, the plugin supports
a cool-down period. After a user uses `/rp`, he or she can't use it again for
a configurable number of seconds. (See **Configuration**, below.)

#### Changing the Configuration

Configuration is self-explanatory. When the plugin comes up, it creates
a `plugins/RandomTeleport` directory, if that directory doesn't exist. If
you want to override the default configuration, download a copy of
`src/main/resources/config.yml` from this repo, edit it, install it in that
directory, and restart or reload your server. The comments in the `config.yml`
file explain what each configuration item does.

### Plugin Permissions

The plugin uses [Vault][] to manage its permissions, which means it's
compatible with all the permissions subsystems supported by Vault. Currently,
there's only one permission, `RandomTeleport.rp`. If a player has that permission,
he or she can randomly teleport. If the permission is missing, it defaults to
"allow".

## Getting the Plugin

You can either download a precompiled version of the plugin or compile it
yourself.

### Downloading the Plugin

 You can download the compiled plugin from
 <http://download.clapper.org/mc-random-teleport/>.

### Building the Plugin

To build the plugin, you'll need a Java JDK (preferably Java 7).

* Check out the repo.
* Run: `./gradlew zip` (or, on Windows, `gradlew.bat zip`). The (local)
  `gradlew` script will handle downloading the appropriate version of Gradle
  and running the build.
* The resulting distribution is in `build/distributions/mc-random-teleport-VERSION.zip`,
  where `VERSION` is the current version of the plugin.

### Installing the Plugin

Unpack the zip file you downloaded or built above.  Unzipping will result in an
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

This plugin also relies on the [Vault][] plugin; you will need to install
Vault separately.

Then, restart or reload your Bukkit server.

[Vault]: http://dev.bukkit.org/bukkit-plugins/vault/
