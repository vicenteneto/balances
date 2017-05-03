(defproject balances "0.1.0"
  :description "A checking account from a bank that allows putting or taking money at any given time."
  :url "http://github.com/vicenteneto/balances"
  :license {:name "MIT License"
            :url  "http://github.com/vicenteneto/balances/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-free "0.9.5561"]
                 [clj-time "0.13.0"]
                 [expectations "2.1.8"]
                 [metosin/compojure-api "1.1.10"]]
  :plugins [[lein-datomic "0.2.0"]]
  :ring {:handler balances.core/handler}
  :datomic {:schemas ["resources" ["schema.dtm"]]}
  :profiles {:dev     {:plugins      [[lein-ring "0.11.0"]
                                      [lein-midje "3.2.1"]]
                       :dependencies [[javax.servlet/servlet-api "2.5"]
                                      [cheshire "5.7.1"]
                                      [ring/ring-mock "0.3.0"]
                                      [midje "1.8.3"]]
                       :datomic      {:config "resources/free-transactor.properties"
                                      :db-uri "datomic:free://localhost:4334/balances"}}
             :uberjar {:aot :all}})
