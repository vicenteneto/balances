(defproject balances "0.1.0"
  :description "A checking account from a bank that allows putting or taking money at any given time."
  :url "http://github.com/vicenteneto/balances"
  :license {:name "MIT License"
            :url  "http://github.com/vicenteneto/balances/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev     {:plugins      [[lein-midje "3.2.1"]]
                       :dependencies [[midje "1.8.3"]]}
             :uberjar {:aot :all}})
