# bisq-engine

Base component for a [BISQ](https://github.com/bisq-network/exchange) MVC architecture.

This bootstraps the core application optionally with GUI or headless without launching the javaFX application and plumbs the Guice injector to an API interface/implementation space.

It is envisaged to structure the model/interfaces in a way to provide maximum ease of re-use between multiple outputs (http,socket,io,etc). The Application is optionally launched under a Spring-boot container to provide an http outlet. It is envisaged that the API commonly serve the native JAVA GUI and any remote GUI's or any automated procedures from a common point.

The initial commit does not do much yet except to wire major components together. I have connected up a http JsonRpc interface for now, working on swagger.

## To regtest:


**Install**

All the install dependencies and requirements are the same as https://github.com/bisq-network/exchange/blob/master/doc/build.md.

```
mkdir bisq-engine
cd bisq-engine
git clone https://github.com/citkane/bisq-engine.git ./
mvn clean install
```

**Launch a bitcoin server**
```
nohup bitcoin-qt -regtest &
```
From the GUI go to 'Help / Debug window / Console' and run `generate 101`

**Start a seednode**
```
nohup java -jar seednode/target/SeedNode.jar --baseCurrencyNetwork=BTC_REGTEST --useLocalhost=true --myAddress=localhost:2002 --nodePort=2002 --appName=bisq_seed_node_localhost_2002 &
```

**Start the engine**
Option flags are `--headless` to start without GUI and `--http <optional port>` or `--http` to start at port 8080. Bound to `localhost` only.

```
java -jar engine/target/engine-0.6.5.jar --baseCurrencyNetwork=BTC_REGTEST --bitcoinRegtestHost localhost --nodePort 2222 --useLocalhost true --appName Bisq-Regtest-Bob --seedNodes=localhost:2002 --headless --http
```

The engine is listening on port 8080.

It can't do much of anything yet, but you can test that the Api is connected, injected and responding in a new console:

```
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"boot"}' http://localhost:8080/api

```
returns some info on the bootstrap state.

```
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"user"}' http://localhost:8080/api

```
returns the user's account id.
