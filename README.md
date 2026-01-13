# Share to folder

This app allows to save files shared by another app to a folder.

It is a convenient companion to apps that only allow to share data, but not save it. It is also useful with apps that have a user interface for sharing that is more convenient than for saving or copying. For instance, in gallery you easily can use share to sort photos with just 2 tapsper photo, whereas using copy would be more cumbersome.

## Features

- folders to which you have shared documents at least once are remembered as shortcuts

- for security, sharing to a shortcutted folder is still subject to a confirmation dialog, but you can answer allow "always" if the target folder is not security critical.

- you can browse your list of recently shared folders in the app's main activity. Red are those that still require a confirmation to share to, and green are those to which you have answered "always". You can remove the pre-authorization again in case you clicked "always" accidentally or made up your mind.

## Installation

Download and install the latest app from the [release section](https://github.com/AlainKnaff/share-to-folder/releases)

However, you may also compile it yourself:

	./gradlew build

