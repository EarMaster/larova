# frozen_string_literal: true

# fastlane, for one job: pushing the store listing in fastlane/metadata/android to Google Play.
#
# Pinned to a minor line rather than floating, because this runs unattended against the live store
# entry and a resolver picking a new major on a Tuesday is not a thing to discover from a listing
# that has changed in fourteen languages.
#
# `Gemfile.lock` is deliberately **not** committed. Nothing in this repository is developed in Ruby
# and nobody runs fastlane locally: the gems are resolved and installed by `play-listing.yml` when
# these files change, which is the only place they are ever used. A lockfile resolved on a Windows
# laptop and consumed by a Linux runner is a file that exists to be wrong.
#
# The app itself needs none of this. Gradle builds and publishes the binary; fastlane never sees
# the AAB.

source "https://rubygems.org"

gem "fastlane", "~> 2.238"
