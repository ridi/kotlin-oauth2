# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2022-10-11
**RxJava version changed from 2.x to 3.x**

### Changed
- Bump `kotlin` version from 1.4.10 to 1.5.21.
- Bump `RxJava` library version from 2.2.20 to 3.1.5.
- Bump `RxAndroid` library version from 2.1.0 to 3.0.0.

## [1.1.8] - 2022-01-17
### Changed
- Replace `jcenter` to `mavenCentral`.

## [1.1.7] - 2021-04-06
### Fixed
- Relocate incorrectly placed default error description.

## [1.1.6] - 2021-03-22
### Changed
- Add default error description to `AuthorizationFailedException`.
- Bump `RxJava` version from 2.2.4 to 2.2.20.
- Bump `jwtdecode` version from 1.2.0 to 2.0.0.
- Bump `retrofit2` and related libraries' versions from 2.6.0 to 2.6.4.

## [1.1.5] - 2020-05-14
### Changed
- Add `extraData` argument to `Authorization::refreshAccessToken`.
- Bump `RxJava` version from 2.2.4 to 2.2.10.

## [1.1.4] - 2020-02-10
### Changed
- Add `extraData` argument to `Authorization::requestPasswordGrantAuthorization`.

## [1.1.3] - 2019-11-06
### Fixed
- Fix crash when response error body is not a JSON string.

## [1.1.2] - 2019-09-26
### Fixed
- ~Fix crash when response error body is not a JSON string.~

## [1.1.1] - 2019-01-14
### Changed
- Bump `RxJava` version from 2.2.1 to 2.2.4.
- Bump `Retrofit` version from 2.4.0 to 2.5.0.
- Explicit license information was added to Maven POM.

## [1.1.0] - 2018-10-02
### Added
- Add password grant authorization.

### Removed
- Remove code grant authorization(obsoleted).

## [1.0.0] - 2018-09-12

**The first stable release! :tada:**
