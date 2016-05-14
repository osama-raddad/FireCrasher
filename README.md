# FireCrasher

FireCrasher is designed to handle the Uncaught Exceptions on the android application and helpe recover from the Exception 
Without exiting from the application.

# Usage :

you need to add this lines to your `build.gradle`


```
repositories {
    maven {
        url 'https://dl.bintray.com/osama-raddad/maven/'
    }
}
```

and

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
