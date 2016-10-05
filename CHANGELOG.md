Change Log
==========

All notable changes to this project will be documented in this file, which
follows the conventions of [keepachangelog.com](http://keepachangelog.com/).
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

...

## [0.2.1] - 2016-10-05

### Added
- Add `envoy.core/clear-accesses!` helper function.

### Fixed
- Implement `clojure.lang.IFn/applyTo` so that compile-time environment lookups
  work properly.

## [0.2.0] - 2016-10-03

### Changed
- Rename `envoy.behavior` ns to `envoy.check`.

### Added
- Add `envoy.core/set-env!` REPL helper function.

## [0.1.0] - 2016-09-30

Initial release.

[Unreleased]: https://github.com/amperity/envoy/compare/0.2.1...HEAD
[0.2.1]: https://github.com/amperity/envoy/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/amperity/envoy/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/amperity/envoy/releases/tag/0.1.0
