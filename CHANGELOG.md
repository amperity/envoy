Change Log
==========

All notable changes to this project will be documented in this file, which
follows the conventions of [keepachangelog.com](http://keepachangelog.com/).
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

## [1.0.0] - 2021-12-27

First production release! There were no breaking changes, we're just finally
admitting to ourselves that the library is stable and unlikely to change
drastically.

### Changed
- Update dependency versions. [#12](https://github.com/amperity/envoy/pull/12)

## [0.3.3] - 2018-11-20

### Fixed
- Fixed bug (introduced in 0.3.2) causing lookups from `:boolean`
  variables to return falsey values as strings.

## [0.3.2] - 2018-09-07

### Added
- Add a `:parser` field to `defenv` that sets a custom function as the
  value parser.

### Fixed
- Fixed bug preventing `EnvironmentMap` from being used with `find`
  and `select-keys`.

## [0.3.1] - 2017-02-02

### Fixed
- Fixed bug preventing `EnvironmentMap` from being used with `merge` and `into`
  via `conj`.

## [0.3.0] - 2017-01-19

### Added
- Add `envoy.core/declare-env-attr!` helper to add new attribute schemas to
  envoy.

### Fixed
- Fixed bug with `defenv` attributes outside the normal schema when `TRACE`
  logging is enabled.

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

[Unreleased]: https://github.com/amperity/envoy/compare/1.0.0...HEAD
[1.0.0]: https://github.com/amperity/envoy/compare/0.3.3...1.0.0
[0.3.3]: https://github.com/amperity/envoy/compare/0.3.2...0.3.3
[0.3.2]: https://github.com/amperity/envoy/compare/0.3.1...0.3.2
[0.3.1]: https://github.com/amperity/envoy/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/amperity/envoy/compare/0.2.1...0.3.0
[0.2.1]: https://github.com/amperity/envoy/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/amperity/envoy/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/amperity/envoy/releases/tag/0.1.0
