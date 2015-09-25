Avahi4J allows a Java application to do make use of the Avahi daemon to publish new services and browse for existing ones. This page presents some simple examples showing how to use Avahi4J to achieve both of these.

# Common code #
Whether you are publishing or browsing, there are some common steps:
  * You must first create a Client object, which represent an Avahi client. this is done with:
```
Client client = new Client();
```
The Client class constructor may throw an exception of type Avahi4JException or an Error, if something goes wrong while creating the client, for example, if the Avhai4J JNI library can not be loaded, or if memory runs out.
  * Once you have a client, you must start it using:
```
client.start();
```

After that, you can either publish or browse services. **But remember that when your client is not needed anymore, you must release it using:**
```
client.release();
```
If you skip this step, this will result in a memory leak !!!

# Service publishing #
To publish a service, you must first decide on a service name, type, port number and possibly some TXT records. In this example, we will use:
  * 1515 as the TCP port number where the service is reachable,
  * `TestService` as our service name,
  * `_test._tcp` as the service type,
  * `record1=1` and `record2=2` as the service's TXT records.

## Creating the entry group ##
The first step to publish this service is to create an [entry group](file:///home/gilles/workspace/avahi4j/doc/avahi4j/EntryGroup.html) to hold the various bits of information about this service. Creating an entry group is done using:
```
EntryGroup group = client.createEntryGroup();
```

## Adding the service ##
Second, add the service to the entry group using:
```
// create the TXT records
Vector<String> txtRecords = new Vector<String>();
txtRecords.add("record1=1");
txtRecords.add("record2=2");

// add service to entry group
group.addService(Avahi4JConstants.AnyInterface, Protocol.ANY, "TestService", "_test._tcp", null, null, 1515, txtRecords);
```
It is recommended that when calling `addService()`, you use:
  * `Avahi4JConstants.AnyInterface` as the interface index: the service will be publish on all available network interfaces,
  * `Protocol.ANY` as the IP protocol type: the service will be announced on all IP protocols supported,
  * `null` as the domain name: the current domain will be used,
  * `null` as the host name: the current hostname will be used.

## Committing the entry group ##
The last step is to commit the group. This will effectively publish all the services in the entry group:
```
group.commit();
```

## Unpublishing the  service ##
The service is now published. It can be updated using `updateService()` if any of the service's detail needs to be changed. The service can be unpublished by calling `reset()`.

**Here again, you MUST call `release()` on the entry group when it is no longer needed.**

## Sample browser application ##
The code demonstrated here is at the core of the `avahi4j.examples.TestServicePublish` sample application, which is shipped with Avahi4J.

# Service Browsing #
Browsing for existing services is extremely simple. All you need is a service type and an object implementing the `IServiceBrowserCallback` interface. In this example, we will use `_test._tcp` as the type, and assume the `this` object implements the `IServiceBrowsercallback` interface. This interface allows Avahi4J to inform one of our objects whenever a service matching the criteria becomes available or is removed.

## Creating a service browser ##
The first step is to create a service  browser:
```
// the first argument "this" represent this object which implements
// the IServiceBrowserCallback interface, and will be notified when
// a service that matches the criteria becomes available or is removed.
ServiceBrowser browser = client.createServiceBrowser(this, Avahi4JConstants.AnyInterface, Protocol.ANY, "_test._tcp", null, 0);
```
**The service browser must be released when no longer needed.**

## Receiving notifications ##
Whenever a service matching the service type (`_test._tcp` in our example) is added or removed, our callback object (`this` in our example) will have its `serviceCallback()` method called:
```
void serviceCallback(int interfaceNum, Protocol proto, BrowserEvent browserEvent, String name, String type, String domain, int lookupResultFlag)
```
The arguments passed to the callback function identify the service that matches the criteria. **You MUST check the `browserEvent` enumeration first. If it is set to `BrowserEvent.FAILURE`, the other arguments are meaningless, and may be `null`.**

## Resolving a service ##
Resolving a service means finding out the IP address (among other things) of the host providing the service. There are two ways of resolving a service. Depending on what you need to know, you can choose one method or the other:
  1. Through the `resolveService()` static method on the `Client` object: choose this method if you simply need to quickly resolve the service, and nothing else. Using `resolveService()` is straight forward: the arguments you must pass to this method are the ones received in the `serviceCallback()` (exactly as received, unless you know what you are doing). Calling this method will block until the result of the query is received. It is spends a bit more "CPU cycles" than creating your own resolver. However, further updates to the service will not be received.
  1. Creating a `ServiceResolver` object: choose this method if you need to stay informed when the service's details change. For example, if you need to watch for updates to the TXT records of a service, or updates to the IP address of the host, you should use this method. Here again, invoke the `ServiceResolver` constructor passing the exact same arguments as received from the service browser callback. The callback object will be notified whenever there is an update to the service's record, be it a change in the address, a change to the TXT records, ...

## Sample browse application ##
The code demonstrated here is at the core of the `avahi4j.examples.TestServiceBrowser` sample application, which is shipped with Avahi4J.




