<a href='https://bintray.com/osama-raddad/maven/fire-crasher?source=watch' alt='Get automatic notifications about new "fire-crasher" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>

[![Stories in Ready](https://badge.waffle.io/osama-raddad/FireCrasher.png?label=ready&title=Ready)](https://waffle.io/osama-raddad/FireCrasher) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-FireCrasher-green.svg?style=true)](https://android-arsenal.com/details/1/3599) [![License Apache](https://img.shields.io/badge/license-Apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0) 

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4da668c9125b401babee42dbb9283f22)](https://www.codacy.com/app/osama-s-raddad/FireCrasher?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=osama-raddad/FireCrasher&amp;utm_campaign=Badge_Grade) <a href="http://www.methodscount.com/?lib=com.osama.firecrasher%3Afirecrasher%3A1.0"><img src="https://img.shields.io/badge/Methods count-83-e91e63.svg"></img></a> <a href="http://www.methodscount.com/?lib=com.osama.firecrasher%3Afirecrasher%3A1.0"><img src="https://img.shields.io/badge/Size-10 KB-e91e63.svg"></img></a> [![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14) [![](https://jitpack.io/v/osama-raddad/FireCrasher.svg)](https://jitpack.io/#osama-raddad/FireCrasher)

# FireCrasher

FireCrasher is designed to handle the Uncaught Exceptions on the android application and help recover from the Exception 
Without exiting from the application.

##Requirements

Min SDK version 14


##Install
Add it in your root build.gradle at the end of repositories:

```js
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Step 2. Add the dependency

```js
	dependencies {
	        compile 'com.github.osama-raddad:FireCrasher:v1.0'
	}
```

##Usage

to use the library add this code to Application class :

```java
public class app extends Application {
    @Override
    public void onCreate() {
        FireCrasher.install(this);
        super.onCreate();
    }
}
```

or you can use your logic, For example :

```java
public class App extends Application {
    @Override
    public void onCreate() {
        FireCrasher.install(this, new CrashListener() {

            @Override
            public void onCrash(Throwable throwable, final Activity activity) {
            
                // show your own message
                Toast.makeText(activity, throwable.getMessage(), Toast.LENGTH_SHORT).show();

                // start the recovering process
                recover(activity);

                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        });
        super.onCreate();
    }
}
```

## Version: 1.0

  * fix some bugs
  * add custom crash Listener

### Let me know!

I’d be really happy if you sent me links to your projects where you use my library. Just send an email to osama.s.raddad@gmail.com And do let me know if you have any questions or suggestion regarding the library. 

## License

    Copyright 2016, Osama Raddad

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
