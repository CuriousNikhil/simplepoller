# simplepoller
A dead simple poller with kotlin coroutines and flow.

[![](https://img.shields.io/badge/mavencentral-0.0.1--alpha-brightgreen)](https://github.com/CuriousNikhil/simplepoller)

## Add dependency

Add dependency to your app level `build.gradle`

```groovy
dependencies {
    implementation 'me.nikhilchaudhari:simplepoller:0.0.1-alpha'
}
```


## Usage
```kotlin
//build poller
val poller = Poller.Builder()
      .get(url = "<URL>")                 // get method and provide url
      .setIntervals(1000, 60000, 2, 2000) // set intervals base, max, delayfactor and delay 
                                          // (or you can use .setInfinitePoll(true) to poll infinitely with provided `delay` value)
      .setDispatcher(Dispatchers.Main)    // Set the dispatcher where you want your result of polling 
                                          // (Please add android/javafx/swing coroutines dependency before if you want to set the Main dispatcher)
      .onResponse {
          textview.text = it.text         // onResponse will be executed on each response received while polling
      }.onError {                        
          Log.e(TAG, it?.message)         // onError will be executed in case of any error
      }.build()                           // build the poller


// Start the poller
poller.start()


// Stop the poller
poller.stop()
```

---
~~~
Note: Documentation is in progress
~~~
