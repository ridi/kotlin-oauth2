# kotlin-oauth2

[![Build Status](https://travis-ci.org/ridi/kotlin-oauth2.svg?branch=master)](https://travis-ci.org/ridi/kotlin-oauth2)
[![Release](https://jitpack.io/v/ridi/kotlin-oauth2.svg)](https://jitpack.io/#ridi/kotlin-oauth2)

JVM base OAuth2 client library written in Kotlin for RIDI account authorization

## Getting started

This library is distributed by [jitpack](https://jitpack.io).

You should add jitpack maven repository to build.gradle file of your project.

```
repositories {
    ...
    maven { url 'https://jitpack.io' }
    ...
}
```

Then you can include this library by adding dependency script to build.gradle file of your project.

```
dependencies {
    ...
    implementation 'com.github.ridi:kotlin-oauth2:<version>'
    ...
}
```

## API

Create `Authorization` object with your client id and client secret.

```kotlin
val authorization = Authorization("client-id", "client-secret")
```

You can get `Single<TokenResponse>` object by following examples.

### Password grant

```kotlin
authorization.requestPasswordGrantAuthorization("username", "password")
```

### Refresh access token

```kotlin
authorization.refreshAccessToken("refresh-token")
```
