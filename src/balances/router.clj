(ns balances.router
  (:require [compojure.api.sweet :refer :all]))

(def api-routes
  (context "/api" []
           :tags ["api"]))
