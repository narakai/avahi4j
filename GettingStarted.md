# Quick start guide #

  * Download the latest stable version from the Downloads tab above and unpack the archive, or checkout the latest version from the SVN repository with
```
svn co http://avahi4j.googlecode.com/svn/avahi4j/trunk avahi4j
```

  * Install a Java JDK 6, Ant and the Avahi client development files. On Ubuntu, this can be done with
```
sudo apt-get install openjdk-6-jdk ant libavahi-client-dev 
```

  * Compile Avahi4J and run a test app with
```
ant clean all test-browse
```
The test application browses the local network for services of type `_test._tcp`

  * In another terminal, run
```
cd avahi4j
ant test-publish
```
This second application publishes a `_test._tcp` service on the local machine. Press Enter and check the published service being resolved in the first terminal.

# Installation #
Once you have successfully compiled Avahi4J and ran the sample applications, you can install Avahi4J on your machine by running with
```
sudo ant install
```
by default, this will copy `avahi4j.jar` in `/usr/share/java` and the JNI library `libavahi4j.so` in `/usr/lib/jni`.

# Using Avahi4J in your own program #
Avahi4J JavaDoc can be generated using
```
ant javadoc
```

Once Avahi4J has been installed, you can start using it in your own program. The only requirement is that you add `avahi4j.jar` to your classpath, and that you add the directory where the JNI library is located to the `java.library.path` property.

This can be done by adding the following arguments when running the JVM:
```
-classpath /usr/share/java -Djava.library.path=/usr/lib/jni
```