# bisq-engine

Base component for a [Bisq](https://github.com/bisq-network/exchange) MVC API architecture.

This bootstraps the core application optionally with the GUI, or headless without launching the javaFX application. It plumbs the Guice injector to an API controller/interface space in a GUI agnostic way.

The model/interfaces are structured to provide maximum ease of re-use of the existing `gui` module dataModels without the need to re-write code or logic. `Engine` is standalone and drop-in capable to the existing `Exchange` stack.

It is envisaged to implement between multiple output types (http,socket,io,etc). The Application is optionally launched under a [Spring-boot](https://projects.spring.io/spring-boot/) container to provide an http output.

At the moment `Engine` provides a [Swagger](https://swagger.io/) UI and endpoints bound to localhost. Swagger is implemented with the [Springfox](https://springfox.github.io/springfox/) library, enabling the use of [Spring framework](https://spring.io/) annotations extended for API documentation. The roadmap includes implementing event driven pushes to API sockets.

## To regtest:


### Install

All the install dependencies and requirements are the same as https://github.com/bisq-network/exchange/blob/master/doc/build.md.

```
mkdir bisq-engine
cd bisq-engine
git clone https://github.com/citkane/bisq-engine.git ./
mvn clean install
```

### Launch a bitcoin server
```
nohup bitcoin-qt -regtest &
```
From the GUI go to 'Help / Debug window / Console' and run `generate 101`

### Start a seednode
```
nohup java -jar seednode/target/SeedNode.jar --baseCurrencyNetwork=BTC_REGTEST --useLocalhost=true --myAddress=localhost:2002 --nodePort=2002 --appName=bisq_seed_node_localhost_2002 &
```

### Start the engine

Option flags are `--headless` to start without GUI and `--http <optional port>` or `--http` to start at port 8080. Bound to `localhost` only.

**Arbitrator** (GUI, no API):

```
java -jar engine/target/Engine.jar --baseCurrencyNetwork=BTC_REGTEST --bitcoinRegtestHost localhost --nodePort 4444 --useLocalhost true --appName Bisq-Regtest-Arbitrator --seedNodes=localhost:2002
```
In the Bisq GUI navigate to 'Account' and press Alt-r. A new 'Arbitrator registration' tab will appear. Click into there and you should have a prefilled key to acknowledge, and then click on 'Register arbitrator'


**Bob** (Headless with API):
```
java -jar engine/target/Engine.jar --baseCurrencyNetwork=BTC_REGTEST --bitcoinRegtestHost localhost --nodePort 2222 --useLocalhost true --appName Bisq-Regtest-Bob --seedNodes=localhost:2002 --headless --http 2000
```
API at http://localhost:2000/swagger


**Alice** (GUI with API):
```
java -jar engine/target/Engine.jar --baseCurrencyNetwork=BTC_REGTEST --bitcoinRegtestHost localhost --nodePort 3333 --useLocalhost true --appName Bisq-Regtest-Alice --seedNodes=localhost:2002 --http 3000
```
API at http://localhost:3000/swagger


If the GUI's are not showing that they are synchronised with REGTEST, go to your bitcoin core and `generate 1`.

## Where it's at?

The plumbing is all done. The API can start from a clean boot, create account, fund wallet, create/accept offers and manage a trade through to completion.

This is the most basic level of Bisq functionality, but the framework is present to build the API out into full functionality.
