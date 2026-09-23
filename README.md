### Group 63
# One Line Picture
One Line Picture is an innovative spinoff of the classic game “One Word Story” in which players create a story together, taking turns, each adding one word to the story each turn.

## Build and test

The following basic commands assume Java 17 or newer.

Compile the application:

```bash
mkdir -p build/classes
javac -d build/classes src/onelinepicture/*.java
```

Run the application:

```bash
java -cp build/classes onelinepicture.OneLinePictureApp
```

To compile and run the tests, set `JUNIT_JAR` to the location of the JUnit 5
standalone console jar first:

```bash
export JUNIT_JAR=/path/to/junit-platform-console-standalone.jar
mkdir -p build/test-classes
javac -cp "$JUNIT_JAR:build/classes" -d build/test-classes test/onelinepicture/*.java
java -jar "$JUNIT_JAR" --class-path build/classes:build/test-classes --scan-class-path
```

The test suite covers model validation, turn rules, timers, replay ordering,
canvas mouse/touch input, malformed network messages, game-ID validation,
join rejection, wrong-turn drawing, multiplayer synchronization, and server-
owned timeout advancement.

Session creation now lets the host choose the seconds per turn and enter a
drawing prompt. The selected time limit is enforced by the server and the
prompt is synchronized to every player and displayed above the canvas.


# MVP SPECIFICATIONS
An MVP of One Line Picture would support players interacting on a shared canvas, each given the opportunity to draw a single line before the game moves to the next player.
| Functional Requirements: | Non-Functional Requirements: | 
|---|---|
| Create a game with a shared blank canvas. | Stop drawing when the player releases the mouse/screen and move to the next player. |
| Advance the turn to the next player once a turn ends | Track cursor location and draw in location when the mouse button is pressed. |
| Only allow players to draw on their turn. | Draw on players turn on click/press. |
| Automatically end a users turn after a set time. | Multi-Device multiplayer. |
| Store drawing changes. | Strokes appear as the cursor moves, not just after release.
| Display the completed drawing. | Canvas and turn state stay synchronized across all devices
| Replay the drawing as it was drawn but sped up | |

The host can customize the turn duration during session setup. A session
prompt is also sent with the start message so every device displays the same
drawing goal.
