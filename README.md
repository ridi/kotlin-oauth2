# kotlin-oauth2

[![Build Status](https://github.com/ridi/kotlin-oauth2/workflows/Build/badge.svg?branch=master)](https://github.com/ridi/kotlin-oauth2/actions)
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

### Android ProGuard

If you want to use this library on your Android project using ProGuard, add the following line to your ProGuard rules file.

```
-keepclassmembers class com.ridi.oauth2.TokenResponse { *; }
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
