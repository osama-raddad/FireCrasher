# FireCrasher

FireCrasher is designed to handle the Uncaught Exceptions on the android application and help recover from the Exception 
Without exiting from the application.

##Requirements

Min SDK version 14


##Installing with [Gradle](http://gradle.org/)

```groovy
dependencies {
...
compile 'com.osama.firecrasher:firecrasher:0.9'
}
```

##Usage

to use the library add this code to Application class

```java
public class app extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FireCrasher.install(this);
    }
}
```

### Version: 0.9

  * Initial Build

#### Let me know!

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
