<a href='https://bintray.com/osama-raddad/maven/fire-crasher?source=watch' alt='Get automatic notifications about new "fire-crasher" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>

[![Stories in Ready](https://badge.waffle.io/osama-raddad/FireCrasher.png?label=ready&title=Ready)](https://waffle.io/osama-raddad/FireCrasher) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-FireCrasher-green.svg?style=true)](https://android-arsenal.com/details/1/3599) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4da668c9125b401babee42dbb9283f22)](https://www.codacy.com/app/osama-s-raddad/FireCrasher?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=osama-raddad/FireCrasher&amp;utm_campaign=Badge_Grade) <a href="http://www.methodscount.com/?lib=com.osama.firecrasher%3Afirecrasher%3A1.0"><img src="https://img.shields.io/badge/Methods count-83-e91e63.svg"></img></a> <a href="http://www.methodscount.com/?lib=com.osama.firecrasher%3Afirecrasher%3A1.0"><img src="https://img.shields.io/badge/Size-10 KB-e91e63.svg"></img></a> [![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14) [![](https://jitpack.io/v/osama-raddad/FireCrasher.svg)](https://jitpack.io/#osama-raddad/FireCrasher)

<a href='https://ko-fi.com/A4763RZL' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://az743702.vo.msecnd.net/cdn/kofi2.png?v=0' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

# FireCrasher

FireCrasher is designed to handle the uncaught exceptions RECOVERY process from the Exception 
Without exiting from the application.

## Requirements

Min SDK version 14


## Install
Add it in your root build.gradle at the end of repositories:

```groove
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Step 2. Add the dependency

```groove
	dependencies {
	        implementation 'com.github.osama-raddad:FireCrasher:1.5.6'
	}
```

## Usage

to use the library add this code to Application class :

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
	FireCrasher.install(this);
    }
}
```

or you can use your logic, For example :

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FireCrasher.install(this, object : CrashListener() {

            override fun onCrash(throwable: Throwable, activity: Activity) {
                Toast.makeText(activity, throwable.message, Toast.LENGTH_SHORT).show()
                // start the recovering process
                recover(activity)

                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        })
    }
}
```

## Contributing

We welcome contributions to FireCrasher!
* ⇄ Pull requests and ★ Stars are always welcome.

## Version: [1.5.4](https://github.com/osama-raddad/FireCrasher/releases/tag/1.5.3)

  * fix backstack bug

## Version: [1.5.3](https://github.com/osama-raddad/FireCrasher/releases/tag/1.5.3)

  * refactor the library
  * fix some bugs

## Version: [1.1](https://github.com/osama-raddad/FireCrasher/releases/tag/v1.1)

  * add Java8 support
  * fix some bugs

## Version: [1.0](https://github.com/osama-raddad/FireCrasher/releases/tag/v1.0)

  * fix some bugs
  * add custom crash Listener

### Let me know!

I’d be really happy if you sent me links to your projects where you use my library. Just send an email to osama.s.raddad@gmail.com And do let me know if you have any questions or suggestion regarding the library. 

## License

    Copyright 2019, Osama Raddad

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
