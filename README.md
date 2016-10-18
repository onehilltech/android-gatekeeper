android-gatekeeper
==================

[![Download](https://jitpack.io/v/onehilltech/android-gatekeeper.svg)](https://jitpack.io/#onehilltech/android-gatekeeper)
[![Build Status](https://travis-ci.org/onehilltech/android-gatekeeper.svg)](https://travis-ci.org/onehilltech/android-gatekeeper)

Gatekeeper support library for Android.

## Installation

#### Gradle

```
buildscript {
  repositories {
    maven { url "https://jitpack.io" }
  }
}

dependencies {
  compile com.github.onehilltech:android-gatekeeper:x.y.z
}
```

## Getting Started

Use the `gatekeeper-cli` to add a new client that represents the mobile application to 
the database. Then, define the following values in `strings.xml`:

```
<!-- make sure to add trailing / -->
<string name="gatekeeper_baseuri">URL for Gatekeeper</string>

<string name="gatekeeper_client_id">CLIENT ID</string>
<string name="gatekeeper_client_secret">CLIENT SECRET</string>
```

## Built-in Activities

### Login

The login activity should be the launcher activity for your application. It will
check if the user is currently logged in. If so, then the login activity will
start the main activity. If not, then the login activity will show the login screen
before proceeding to the main activity. Define the 
`com.onehilltech.gatekeeper.android.LOGIN_SUCCESS_REDIRECT_ACTIVITY` meta-data
attribute in the login activity to specify the main activity to start after
the login process is complete.

```
<activity
    android:name="com.onehilltech.gatekeeper.android.SingleUserLoginActivity"
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>

    <meta-data
        android:name="com.onehilltech.gatekeeper.android.LOGIN_SUCCESS_REDIRECT_ACTIVITY"
        android:value=".MainActivity" />

    <meta-data
        android:name="com.onehilltech.gatekeeper.android.NEW_ACCOUNT_ACTIVITY"
        android:value="com.onehilltech.gatekeeper.android.NewAccountActivity" />
</activity>
```

## Custom Activities
