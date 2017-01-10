# Change Log for mc-random-teleport

Version 0.6.1

* Updated to latest version of `mc-scala-plugin`.

Version 0.6.0

* Updated to use new `ScalaPlugin` class from `mc-scala-plugin`.

Versions 0.5.0, 0.5.1, 0.5.2

* Updated to Bukkit 1.11 and rewritten to use new (personal) Scala helper 
  libraries.

Version 0.5

* Updated to 1.7.9

Version 0.4

* Added `gradlew`, so building does not require downloading Gradle first.
* If permission is missing, default to allow, not deny.
* Plugin is now generated.

Version 0.3

* Change to random teleport algorithm to minimize (but not eliminate)
  the probability of teleporting into a cave, because teleporting above
  ground is considerably more interesting.
* Added permissions.

Version 0.2

* Change to height calculation, to try to avoid having player materialize
  inside solid material or inside a dark cave.

Version 0.1

* Initial release
