<p align="center">
<img src='https://cdn-images-1.medium.com/max/2600/1*7CVLni2XSYNFzy7dRHLtsQ.png'/>

<a href='https://bintray.com/osama-raddad/maven/fire-crasher?source=watch' alt='Get automatic notifications about new "fire-crasher" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>

 <a href="http://www.methodscount.com/?lib=com.osama.firecrasher%3Afirecrasher%3A1.0"><img src="https://img.shields.io/badge/Methods count-83-e91e63.svg"/></a> <a href="http://www.methodscount.com/?lib=com.osama.firecrasher%3Afirecrasher%3A1.0"><img src="https://img.shields.io/badge/Size-10 KB-e91e63.svg"/></a>

<a href='https://ko-fi.com/A4763RZL' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://az743702.vo.msecnd.net/cdn/kofi2.png?v=0' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>
</p>

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4da668c9125b401babee42dbb9283f22)](https://www.codacy.com/app/osama-s-raddad/FireCrasher?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=osama-raddad/FireCrasher&amp;utm_campaign=Badge_Grade)

[![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14) [![](https://jitpack.io/v/osama-raddad/FireCrasher.svg)](https://jitpack.io/#osama-raddad/FireCrasher)

[![Stories in Ready](https://badge.waffle.io/osama-raddad/FireCrasher.png?label=ready&title=Ready)](https://waffle.io/osama-raddad/FireCrasher) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-FireCrasher-green.svg?style=true)](https://android-arsenal.com/details/1/3599) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# FireCrasher

FireCrasher is designed to handle the uncaught exceptions and utilize a RECOVERY process from the Exception 
Without exiting from the application.

## The Problem

"The study, carried out online by uSamp, found that freezing (76%), crashing (71%) and slow responsiveness (59%) were the primary bugbears when it came to app problems, with heavy battery usage (55%) and too many ads (53%) also mentioned. Users stressed that performance mattered the most on banking apps (74%) and maps (63%), with the latter no doubt much to the chagrin of Apple, which has had some difficulty with its own maps software on iOS 6. For almost every respondent (96%) said that they would write a bad review on an under-par app, while 44% said that they would delete the app immediately. Another 38% said that they would delete the app if it froze for more than 30 seconds with 32% and 21% respectively indicating that they would moan about the app to their friends or colleagues in person or over Facebook and Twitter. A considerable 18% would delete an app immediately if it froze for just five seconds, but 27% said that they would persist with the app if they paid for it. Those experiencing bad apps urged developers to fix the problem (89%) first and foremost, followed by offering easy refunds (65%) and a customer service number (49%)."

## The Solution

Every developer knows that shit happens, and at some point you will ship a random exception to the production code (application) thus you might risk losing a 44% of the affected users, this where Firecrasher comes in, it utilizes a recovery sequence to limit and chicaneries the crash consequences on three different levels; the first level is the random behavioral crash (occasional crash) that would be solved with just restarting the crashed activity if the restarted activity kept crashing for three consecutive times, the second level of the sequence will start executing at this stage the crashed is considered dead and the library checks if there are other activities in the backstack it invokes the onBackPressed() if there are no activities in the backstack then level three takes effect in restarting the whole application from the default activity. Moreover, it works without losing any crash reports.

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
	        implementation 'com.github.osama-raddad:FireCrasher:2.0.0'
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

            override fun onCrash(throwable: Throwable) {
                Toast.makeText(this@App, throwable.message, Toast.LENGTH_SHORT).show()
                // start the recovering process
                recover()
                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        })
    }
}
```

to detarmein the crash level before srating the recovery you can use:
```kotlin
       FireCrasher.install(this, object : CrashListener() {

            override fun onCrash(throwable: Throwable) {

                evaluate { activity, crashLevel ->
                     recover {
                                Toast.makeText(this@App, "recover", Toast.LENGTH_LONG).show()
                            }
                	}
                
                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        })
```
## Contributing

We welcome contributions to FireCrasher!
* ⇄ Pull requests and ★ Stars are always welcome.

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
