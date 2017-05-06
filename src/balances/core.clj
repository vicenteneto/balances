(ns balances.core
  (:require [compojure.api.sweet :refer [api]]
            [balances.router :refer [api-routes]]))

(def handler
  (api
    {:swagger
     {:ui   "/"
      :spec "/swagger.json"
      :data {:info {:title       "Checking account service"
                    :description "A checking account from a bank that allows putting or taking money at any given time"}
             :tags [{:name "api"}]}}}

    api-routes))
