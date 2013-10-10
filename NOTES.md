TODO: When you examine the message pattern per app, make sure you mention the cost of over the wire serialisation when dealing with pure clojure messages, and the fact tht camel will probably serialize stuff even when it's just moving to the next processor locally 

# Introduction #

# Areas of Interest #

## Systems Integration ##

The Daily Mail is in the process of replacing a Java/Spring/Oracle architecture with a heterogeneous micro-service architecture. We have applications written in Clojure, Node and Ruby as well as much of the original stack in place. These apps in turn talk to a range of databases and message brokers such as ElasticSearch, Redis, ActiveMQ and Hornetq. This can be quite a headache when it comes to integrating the systems. Forgetting any business requirements we have to satisfy, we're also stuck with the time-consuming process of evaluating client libraries and writing bootstrap code to speak all these protocols. Any integration framework that removes this requirement wins big points from me.

## Business Rules Capture ##

One of the advantages of a message oriented architecture is that applications do not need to understand where they're dispatching their messages to, they only need to fire and forget. This kind of decoupling of applications is powerful, but it does mean that a large chunk of business logic, EG routing, is encapsulated in the integration layer. We like our integration systems to scale, and that means that new data sources enter the equation often, as do consumers of that data. Being able to accurately gauge what components are in play is a real advantage.

Integration frameworks provide different means to analyse these changing topologies, and in judging frameworks I find myself asking the following about it's topology representation.

* Does illustrate the business rules, or just implement them.
* Can I show this to a colleague
* Can I show this to my boss
* Can I change this easily

## Distributed Systems Orchestration ##

This wouldn't hurt to be a spreadsheet/checklist
* Ensured delivery
  * this is potentially the most important requirement of the systems we build. The asynchronous model of message bound communications means that we say godbye to the safety net of synchronous transaction models. If we fire and forget messages into a stream, we still need assurance that  our messages are not forgotten by the intended receivers.
* Distributed workload

* Throttling
* collaboration (IE map/reduce)
* redundancy
* Mediation
  * discovery of other applications and streams


### Supporting Concepts ###
* Sources
* Destinations
* Transformations
* Side Effects

# Enterprise Integration Patterns #

In addition to the key areas of interest outlined earlier, I will also be making occasional reference to the book "Enterprise integration patterns".

This book has become a mainstay on the tech bookshelves of those whose work involves enterprise integration, and like other works proferring "patterns" such as the famous gang of four book, it attempts to generalise a set of problems and offers pre-baked solutions.

Hopefully those who are unfamiliar with this book won't find these references off putting. It is an interesting book, worth skimming t the least. I do find it gives a useful taxonimy of integration problems, and it has clearly been a strong influence on many of the products we'll be lookking at today.

It's also probably the case, that as with any generalisation, there are better approaches to the problems we face day-to-day as software makers, and I hope to touch upon some areas where the products under examination have provided solutions that are not recognised as patterns.

EIPs can and often do describe the underpinnings of what I am terming Systems Integartion, and they are very much about distributed systems orchestration. They are also a fine language for business rules capture.

# Case Study #

I'm going to walk through a relatively simple integration problem. It's a real world example from the MailOnline. The characteristics of the problem are that it is:

* Very low throughput (< 5 messages a second) at peak
* Contrained by a resistence to change any of the integration points in any way
* a replacement for an existing system
* It's a stream of data, expected to flow in one direction. Data will be 'poured' into the stream for a couple of sources, and flow towards a single terminal point.
* It's a headache, but not life threatening if a message gets lost every two days, but we can't tolerate a higher error rate than that


Then diagram

This example app is composed of the following patterns (probably more)

Message
* The discrete unit of work within the stream.

Message Channel
* A point of exchange in a data stream. In this case these are at the start and end of the stream. 

Message Broker
* The "Hub-And-Spoke" architecture, a centralised depot of messages, to which interested applications subscribe.

Message Router
* A flow control construct / conditional router

Content Enricher
* enrich the message content

Pipeline
* synchronous handling of messages by many receivers

Splitter
* separation of a message into composite parts, and emission of independent messages for those parts

Aggregator
* aggregation of multiple messages into a single message



Any high level view of the tools we're going to look at today reveals that for the most part they are implementing something close to the patterns described in the EIP book. Let's name those patterns before we continue:

# Side by Side comparisons#

#Mule#

I'm going to start with a product from MuleSoft imaginitively named Mule. This is a fully fledged Enterprise Service Bus, an application into which sub-applications can be plugged, that provides sandboxing between those pieces, but a shared set of resources with which to work. I don't know much about Mule, so I won't spend too much time on it.

The reson Mule is in this talk at all, is because it was the solution used at the Mail until reasonably recently. The case study we're lookingat was originally implemented in Mule, but we had problems with it.

1. No one could understand it.

The developers we have, and th development "processes" we have, are oriented to looking at code in editors. We have eschewed complex modelling in vast XML files in favour of sensibly sized services with clean and succinct code in them. Mule, by contrast, inclines towards development of integration solutions via either:

* a WYSIWYG IDE, which is no doubt very cool and clever but leaves me feeling cold.
* 3000+ lines of Spring XML plus domain objects, converter classes, etc

2. No one wanted to understand it.

See above, we had one developer who managed to get a handle on it over time, but he was frequently heard to say "I don't want to be the Mule guy".

3. It was breaking and no-one could figure out why.

Multicasting, that was the answer in the end. When we tried to scale horizontally Mule tried to communicate across nodes using Hazelcast, a multicasting library, and this was not possible o our network infrastructure. But no-one could figure this out until long after we'd stopped using Mule.

If you undestand Mule, or if you're inclined to understand it, then its probably a very cool product. I will say that nothing else we talk about today will solve the problem of application sandboxing that Mule does, so there's that. I think you can probably get Mule to communicate well withClojure applications, but you will have a hard time getting it to talk to Clojure developers.

#Camel Holy Grail#
No, it's not a Monty Python refereces, the name derives from conversations inside the Mail regretably. 

Caml is an apache Java library. It is a very thourough approach to the integration problem, using the EIP book as a guide, and provides out of the box solutions to all 59 of the patterns listed. As well as this, it has adaptor libraries for a plethora of web services and message brokers. It is a very mature library, but it's got a lot of classes and calling it from clojure via interop can look very messy.

There are a couple of camel clojure DSLs already in the marketplace, but for one reason or another they didn't appeal to us, so we wrote our own. This is a thin DSL on top of what is called the fluent builder syntax, which is a method chaining syntax which looks like this:

public class MyRouteBuilder extends RouteBuilder {
   public void configure() {
        from("http://foo")
        .process(MyBean.class)
        .to("activemq:my.queue.name");
}

We translate that a bit so that it looks a bit more clojure-thready:

(defroute (make-context)
  (from "http://foo")
  (process (processor ...))
  (to "activemq:my.queue.nae"))

And we provide some wrappers around the building-block classes camel provides, which e think is enough to allow you to build any solution, and to leverage Camel's routing and adapter libraries.

OK fine. So what are the characteristics of a camel based solution to this problem:

SHOW CODE, annotate and discuss. Fraz, list the salient points here.

Systems integration
* Very string on systems integration
* HTTP out of the box
* AMQ via maven


Orchestration
* well established approach to error handling
* scales up very well by means of a broker/message channel as an orchestration point
  * Notably, we're unable to distribute amongst nodes in our cluster or co-ordinate amongst them without the broker.
* poor support for aggregation really
Patterns
Message
* It's an instance of the Message instance
* body + headers (much like an http req/res)
* headers can be strings
* body can be byte array, string or anything from javax/jms/Message
  * probably best to stick to strings or bytes, or else you're in a worls of serialisation pain
Message Channel
* camel endpoints
* very nicely vivified from URL strings
Message Broker
* ActiveMQ for us
* Need not be a message broker really, but something that can support queing and is in turn suppoted by camel

Message Router
* Dispatch to named queues via when statements
* first icky case where camel terminology conflicts with clojure terminology

Content Enricher
* a processor that calls set-body

Pipeline
* anything within the defroute defaults to pipelining

Splitter
* via split

Aggregator
* via aggregate


Busines Rules capture
* DSL is quite good at rules capture, uses english style terminology over tech jargon much of the time
* camel terms conflict with clojure fn names

#Lamina#
* lets refactor bootstrap to use lamina channels
* map channel wps-events
* then lets have a new pipeline to do splitting
  * and pipe that into wps-events
  * simples

* because lamina pipelines are functions, it's easy to dispatch into them from other pipelines as sub-workflows

* notably I can't have the HTTP input step inside the pipeline - or can I: look at read-channel
How does Lamina surface errors? how do all solutions.
  * error-handler - nice and simple, but enqueueing behaviours up to me

#Orchestration
Message
* any old clojure you can find
* but if you need to exit the lamina pipeline, serialisation is your own concern

Message Channel
* lamina provides in memory 'channels'

Message Broker
* nah dawg, you gots to do this yourself.

Message Router
* 

Content Enricher
Pipeline
Splitter
Aggregator

##Notes##

Provides cool analytics tools to report on the stream throughput

