# android-oauth2

[![Build Status](https://travis-ci.org/ridi/android-oauth2.svg?branch=master)](https://travis-ci.org/ridi/android-oauth2)
[![Release](https://jitpack.io/v/ridi/android-oauth2.svg)](https://jitpack.io/#ridi/android-oauth2)

Convenient oauth2 library for Android platform

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
    implementation 'com.github.ridi:android-oauth2:<version>'
    ...
}
```

## API

Create `TokenManager` object.

```
var tokenManager = TokenManager()
```

Then, you can get access token from `getAccessToken()` method.
You should set some variables before `getAccessToken()` method is called.

|Variables | Type | Description       |
|-----------------------|-----------------------|---------------------------|
|tokenEncryptionKey|String|Defaults to `null`. `TokenEncryptionKey` should be 16 bytes long.|
|tokenFile|String|The Location where you want to save a token file|
|clientId|String|Client's identifier|
|sessionId|String|Logged-in user's session value|

If you called `getAccessToken()`method successfully, you receive an access token. Otherwise, you receive errors. 

|Errors | Description|
|-------|------------|
|UnexpectedResponseException|Access token is unavailable.|
|InvalidTokenFileException|Token File is deleted or damaged.|
|InvalidTokenEncryptionKeyException| `tokenEncryptionKey` is not 16 bytes long.|

