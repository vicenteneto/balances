(ns balances.core
  (:require [compojure.api.sweet :refer [api]]
            [balances.router :refer [api-routes]]))

(def handler
  (api
    {:swagger
     {:ui   "/"
      :spec "/swagger.json"
      :data {:info {:title       "Checking account service"
                    :description "A service for checking accounts."}
             :tags [{:name "api"}]}}}

    api-routes))
