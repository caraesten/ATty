# ATty
### A telnet client for Bluesky

## Installation
 - Make sure you have a JDK installed. I recommend Temurin, install it [here](https://adoptium.net/temurin/releases/).
 - Clone the repo using git (or just download it as a zip file)
 - From the root directory, run `./gradlew run` (on Mac/Linux) or `gradlew.bat run` (on Windows)

## Use
 - Using your telnet client of choice, open a telnet session to 127.0.0.1
 - By default, ATty runs on the standard telnet port, 23
 - If you want to change this, edit `port` in `gradle.properties`
 - Press return to advance the screen or to send data to the server

## What works
 - Home timeline
 - Notification timeline
 - Creating new posts
 - Replying to posts
 - Liking posts
 - Re-sharing posts
 - Quoting posts

## What doesn't work
 - Threads
 - Images (even with alt text)
 - Custom algorithms
 - Viewing more than 10 posts at a time (pagination)
 - Following / unfollowing
 - Blocking / muting

## Roadmap
 - Early public release
 - Thread unrolling
 - Images or alt text
 - Adjustable number of posts

## Supported devices
 - ATty just uses UTF-8, meaning it should work on most ASCII terminals
 - It also includes (hacky, limited) support for PETSCII
 - Codepoints outside of ASCII will not render correctly on older terminals
 - Future releases may add a compatibility mode to strip those characters

## Libraries
 - ATty is built on the amazing [bsky4j](https://github.com/uakihir0/bsky4j) library
 - It also uses extensive code from my other telnet project, New Session / [dial-a-zine](https://github.com/caraesten/dial_a_zine2/tree/main)