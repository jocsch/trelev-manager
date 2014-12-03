h1 TRELEV Manager

Trelev is an application which allows to create events to which users can subscribe.
It consists of two parts:

* The Trelev Manager
* The Trelev JS Code

You currently look at the manager repository.
The manager offers a REST API and is based on Akka, Akka Persistence and Spray.

Even though this project runs in production for small communities it is relatively quickly thrown together 
(you are probably familiar with these sort of side projects where you start with high ambitions but then
time flies, frameworks start making trouble and you just do the 60% necessary to launch it).

So please don't be to picky on the code you find. I hope that this project will be used during the next years and I can start
to improve it.

The most urgent TODOs:

- [] Unit tests
- [] Propper logging
- [] Better parsing of the e-mail template config (maybe switch to a templating engine)
- [] Creation + Update dates in the events
- [] More flexible JSON serializer for events
- [] Better utilization of the akka framework: One Actor per Event? Subactors for e-mail sending?


h2 Setup

To run the application you only need to issue a

sbt run

To package it for usage on the server please use the native packager plugin:

sbt universal:package-zip-tarball

Beforehand you might want to customize the configuration in /conf/trelev.conf

A separate DB is not required, Akka Persistance creates a LevelDB on the fly to persist the events.
