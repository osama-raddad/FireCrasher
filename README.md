# FireCrasher

FireCrasher is designed to handle the Uncaught Exceptions on the android application and help recover from the Exception 
Without exiting from the application.

# Usage :

you need to add this lines to your `build.gradle`


```
dependencies {
...
compile 'com.osama.firecrasher:firecrasher:0.9'
}
```

then to use the library add this code to Application class

```
public class app extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FireCrasher.install(this);
    }
}
```

# License :

Copyright 2016 Osama Raddad

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
