A simple Minecraft Bukkit server plugin that allows users to change their
display names. Written primarily to play around with Josh Cough's
[Scala Bukkit plugin API](https://github.com/joshcough/MinecraftPlugins).

## Building

* Check out the repo.
* You'll need a recent version of [Gradle](http://gradle.org)
* Run: `gradle zip`
* The resulting distribution is in `build/distributions/nickname.zip`.

## Installation

Unpack the `nickname.zip` file you built above. The zip contains three jar
files:

- The Scala Library plugin, which provides the Scala runtime library to
  Scala Bukkit plugins.
- The Scala Plugin API jar file.
- `nickname.jar`, which contains the code for this plugin.

Copy all three jars to your server's `plugins` directory, adjust any
permissions (see below), and restart or reload your server.

## Permissions

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
