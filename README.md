# lww-node

Distributed last-write-wins set in Clojure with REST API.

[LWW element set](https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type#LWW-Element-Set_(Last-Write-Wins-Element-Set)) implementation in clojure.

> LWW-Element-Set is similar to 2P-Set in that it consists of an "add set" and a "remove set", with a timestamp for each element. Elements are added to an LWW-Element-Set by inserting the element into the add set, with a timestamp. Elements are removed from the LWW-ELement-Set by being added to the remove set, again with a timestamp. An element is a member of the LWW-Element-Set if it is in the add set, and either not in the remove set, or in the remove set but with an earlier timestamp than the latest timestamp in the add set. Merging two replicas of the LWW-Element-Set consists of taking the union of the add sets and the union of the remove sets. When timestamps are equal, the "bias" of the LWW-Element-Set comes into play. A LWW-Element-Set can be biased towards adds or removals. The advantage of LWW-Element-Set over 2P-Set is that, unlike 2P-Set, LWW-Element-Set allows an element to be reinserted after having been removed

## Architercture

This implementation uses clustering architercture with active replication across nodes (virtual synchrony)
with data offloading to redis db using scheduler. In default configuration offloading happens once every minute
or on node shutdown. In current implementaion replication happens immediately upon insert/delete request from client
and is a subject to further improvement.

Implementaion is based on [lww-element-set](https://github.com/edvorg/lww-element-set) library developed by the same author.

## Demo

Demo instance is available at https://lww-node.clj.rocks.

## Scaling

This architecture is scalable horizontally and allows to load nodes evenly using balancer.
Replication algorithm can be further relaxed and optimized:
- by replicating data less often
- by batching replication messages
- by using low-level networking protocols that have less overhead then http

It's possible to achieve as many as 1 million concurrent chaos monkeys as long
as set is sufficiently distributed and balanced across several nodes.
Performance of single node is a subject to further analysis.

## Prerequisites

Make sure that you have JVM and docker installed.

## Running

There are several ways to run the project depending on what you're trying to do.

### Tests

A good thing to start with is to run testing environment and run integration tests against it.
```shell
cd ./docker
./start-env.sh --integration # spin up redis, 3 lww-set nodes (ports 3001-3003) and client viewer monkey
./lein test
./stop-env.sh --integration # shutdown environment
```

During tests you can monitor set data using command

```shell
docker logs -f lww-set-client-viewer-monkey-1
```

This monkey choses random node in cluster of three nodes and monitors it's data for changes.
Data monitoring is implemented using polling and can be further improved using web-sockets
in web or tcp/ip in native client. client-viewer monkey only polls a diff of data since the last poll.

### Prod build

To run prod build on your machine you may simply execute:

```shell
cd ./docker
./start-env.sh --prod # spin up redis and 1 lww-set node on port 3001
...
./stop-env.sh --prod # shutdown environment
```

### Staging build

To run staging build:

```shell
cd ./docker
./start-env.sh # spin up redis and 3 lww-set nodes ports (3001-3003) and several chaos monkey bots.
...
./stop-env.sh # shutdown environment
```

Following chaos monkey bots are implemented:

#### Normal chaos monkey

Sends insert/delete requests to random nodes at random intervals.
To see all running normal chaos monkeys:

```shell
>> docker ps -a | grep lww-set-normal-monkey
>> 563810898d6c        lww-set-builder     "sh -c 'chmod o+w /v…"   Less than a second ago   Up 3 seconds                                 lww-set-normal-monkey-3
>> fda4b072f8ad        lww-set-builder     "sh -c 'chmod o+w /v…"   Less than a second ago   Up 5 seconds                                 lww-set-normal-monkey-2
>> 2634bc8c7858        lww-set-builder     "sh -c 'chmod o+w /v…"   Less than a second ago   Up 6 seconds                                 lww-set-normal-monkey-1
```

To see logs from chaos monkey execute
```shell
>> docker logs -f lww-set-normal-monkey-1 # or any other container id
```

#### offline-to-online chaos monkey

Simulates client that is making lots of changes offline and the synchronizes it with server.
To see all running offline-to-online chaos monkeys:

```shell
>> docker ps -a | grep lww-set-offline-online
>> c88656fbd092        lww-set-builder     "sh -c 'chmod o+w /v…"   About a minute ago   Up About a minute                            lww-set-offline-online-monkey-1
```

To see logs from chaos monkey execute
```shell
>> docker logs -f lww-set-offline-online-monkey-1 # or any other container id
```

#### client-viewer chaos monkey

Simulates client that is observing changes to set in realtime.
To see all running client-viewer chaos monkeys:

```shell
>> docker ps -a | grep lww-set-client-viewer
>> 2adf26321314        lww-set-builder     "sh -c 'chmod o+w /v…"   3 minutes ago       Up 3 minutes                                 lww-set-client-viewer-monkey-1
```

To see logs from chaos monkey execute
```shell
>> docker logs -f lww-set-client-viewer-monkey-1 # or any other container id
```

### Run monkeys on external nodes

To run monkeys locally but connect them to remote cluster you may do following:

```shell
cd ./docker
./start-env.sh --external-nodes http://rust.cafe:3001 --nodes-count 0
...
./stop-env.sh --external-nodes http://rust.cafe:3001 --nodes-count 0
```

External nodes adds additional set of hosts to list of nodes that monkeys connect to.

You may as well start your local nodes and these nodes will replicate to external nodes:

```shell
cd ./docker
./start-env.sh --external-nodes http://rust.cafe:3001 --nodes-count 1
...
./stop-env.sh --external-nodes http://rust.cafe:3001 --nodes-count 1
```

Currently external nodes would have no knowledge of your local node.
This can be improved with dynamic discovery technique.
Also your local instance would have local redis instance for persistence.
This can easily be improved by adding option to connect to external redis service.

### Other deployment options

`./start-env.sh` script is a flexible and allows various models of execution.
Following arguments are supported:

- `-n|--no-cache`
  Disables caching when building docker containers.

- `-v|--verbose`
  Prints debug information during docker env build process.

- `-ub|--use-builder (yes|no)`
  By default all clojure code is build inside docker container.
  `--use-builder no` allows to build it on host.
  Please see `./docker/00-builder/Dockerfile` for the list of dependencies that
  should be present on your system.

- `-s|--start (yes|no)`
  Start all subsystems (redis, lww nodes, all types of monkeys).

- `-sn|--start-nodes (yes|no)`
  Start nodes.

- `-snm|--start-normal-monkey (yes|no)`
  Start normal monkey bot.

- `-soom|--start-offline-online-monkey (yes|no)`
  Start offline-online monkey bot.

- `-scvm|--start-client-viewer-monkey (yes|no)`
  Start client-viewer monkey bot.

- `-nc|--nodes-count (number)`
  Number of lww nodes to star.

- `-nmc|--normal-monkey-count (number)`
  Number of normal monkey processes to start.

- `-nmipc|--normal-monkey-in-process-count (number)`
  Number of normal workers per process to start.

- `-oomc|--offline-online-monkey-count (number)`
  Number of offline-online monkey processes to start.

- `-oomipc|--offline-online-monkey-in-process-count (number)`
  Number of offline-online workers per process to start.

- `-cvmc|--client-viewer-monkey-count (number)`
  Number of client-viewer monkey processes to start.

- `-cvmipc|--client-viewer-monkey-in-process-count (number)`
  Number of client-viewer workers per process to start.

- `-p|--prod`
  Start 1 node, 1 redis, no monkeys.

- `-i|--integration`
  Start 3 nodes, 1 redis, no monkeys.

- `-en|--external-nodes (nodes list)`
  Connect to external nodes.

If you prefer to store these options on disc you may execute
```shell
cp -f /docker/local.example /docker/local
```
And edit `/docker/local` file to your needs.
Options from that file are sourced into bash process every run.

### Quick deployment steps on fresh bare-metal machine
```shell
# login to machine using ssh
sudo apt get install -y docker.io git default-jdk
sudo gpasswd -a ${USER} docker
# relogin
git clone git@github.com:edvorg/lww-node.git
cd lww-node/docker
./start-env --prod
```
## Usage

This implementation uses `application/transit+json` as data interchange format.
All requests and responses are encoded using transit json.

Transit libraries for non-clojure clients:
- java: https://github.com/cognitect/transit-java
- javascript: https://github.com/cognitect/transit-js

Every node exposes following endpoints:

### `POST /insert`

Expects an array of elements encoded in transit format, returns "ok" if succeeded.
Example inserts number 11 and string "foo":

```shell
>> curl -H "Content-Type: application/transit+json" -X POST -d '[11, "foo"]' http://localhost:3001/insert
>> ok
```

### `POST /delete`

Expects an array of elements encoded in transit format, returns "ok" if succeeded.
Example deletes number 11 and string "foo":

```shell
>> curl -H "Content-Type: application/transit+json" -X POST -d '[11, "foo"]' http://localhost:3001/delete
>> ok
```

### `GET /`

Returns set of elements in transit format. Example returns a set with element `6ce99d46-37f8-475c-9e5d-f3a9ecdb5cc4`:

```shell
>> curl -X GET http://localhost:3001/
```
raw response:
```shell
>> ["~#set",["6ce99d46-37f8-475c-9e5d-f3a9ecdb5cc4"]]
```
decoded transit:
```
#{"6ce99d46-37f8-475c-9e5d-f3a9ecdb5cc4"}
```

### `GET /updates`

Returns operations that happened after specified time.
Example returns all changes ever made to set since beginning of unix epoch.

```shell
>> curl -X GET http://localhost:3001/updates?since=0
```
raw response:
```shell
>> ["^ ","~:add-set",["^ ","6ce99d46-37f8-475c-9e5d-f3a9ecdb5cc4",1520698550268],"~:del-set",["^ ","d9e20090-83d4-4950-8c79-f39e681e1b38",1520698545740]]
```
decoded transit:
```clojure
{:add-set {"6ce99d46-37f8-475c-9e5d-f3a9ecdb5cc4" 1520698550268}
 :del-set {"d9e20090-83d4-4950-8c79-f39e681e1b38" 1520698545740}}
```

### `POST /update`

`/update` endpoint is used mostly for replication or for offline-to-online synchronization.
Expects a replica encoded in transit format, returns "ok" if succeeded.
Example inserts number 11 at timestamp 0:
Initial replica:

```clojure
{:add-set {11 0}
 :del-set {}}
```

Encoded replica:
```shell
["^ ","~:add-set",["^ ","~i11",0],"~:del-set",["^ "]]
```

```shell
>> curl -H "Content-Type: application/transit+json" -X POST -d "[\"^ \",\"~:add-set\",[\"^ \",\"~i11\",0],\"~:del-set\",[\"^ \"]]" http://localhost:3001/update
>> ok
```


## List of improvements

- use more relaxed replication technique in order to unload cluster
- improve errors handling for incorrect request data
- add authentication
- add better in-cluster discovery algorithm (currently every node has to be run with static list of all other nodes)
- add ssl certificates
- add option to use external redis service (useful for --prod mode)
- use different naming conventions for containers based on mode (prod, staging, test) to allow running several non-verlapping environments at the same time
- make sure offloading to redis doesn't happen at the same time on nodes by tweaking scheduler
- add `application/json` content type support to endpoints
- tweak jvm memory options for production environment
- `POST /update` endpoint is a dangerous method that should be used for replication only and probably should not
  be exposed to client. Find a better alternative for offline to online synchronization. Maybe the same version but
  with limited range of operation timestamps would work good enough. It would prevent from attacks like deleting
  an element with timestamp 0 which would prevent from ever inserting element in set or opposite inserting an
  element with timestamp LONG_MAX which would prevent from ever deleting element from set.

## License

Copyright © 2018 Edward Knyshov

Distributed under the Eclipse Public License either version 2.0 or (at
your option) any later version.
