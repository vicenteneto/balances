# Balances

A checking account from a bank that allows putting or taking money at any given time.

## Installation

To build **Balances** from scratch on OSX or Linux:

* Install [lein](https://leiningen.org/)
* Install [datomic-free](https://my.datomic.com/downloads/free)
* add path to datomic in your ~/.lein/profiles.clj
    * {:user {:datomic {:install-location "/path/to/your/install/dir/datomic-free-0.9.5561"}}}
* Download [balances](https://github.com/vicenteneto/balances)

```bash
$ git clone https://github.com/vicenteneto/balances.git
$ cd balances
```

## Usage

* lein datomic start &
* lein datomic initialize
* lein ring server
* At this point you should be able to go to http://localhost:3000/index.html in your browser and interact with the service

## Tests

* lein midje

## License

Copyright © 2017 -

Distributed under the MIT License either version 0.1.0 or any later version.
