(ns balances.router
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok]]
            [balances.schemas :refer :all]
            [balances.db :refer :all]
            [schema.core :as s]))

(def api-routes
  (context "/api" []
    :tags ["api"]

    (POST "/transactions" []
      :return Transaction
      :body [transaction (describe NewTransaction "new transaction")]
      :summary "Creates an transaction in the system"
      (ok (add-transaction transaction)))

    (GET "/transactions" []
      :return [Transaction]
      :summary "Gets all transactions"
      (ok (get-transactions)))

    (GET "/transactions/:id" []
      :path-params [id :- Long]
      :return (s/maybe Transaction)
      :summary "Gets all details relevant to a transaction"
      (ok (get-transaction id)))

    (GET "/account/:number/balance" []
      :path-params [number :- s/Int]
      :return (s/maybe Account)
      :summary "Gets the current balance from a giving account"
      (ok (get-balance number)))

    (GET "/account/:number/statement" []
      :path-params [number :- s/Int]
      :return (s/maybe Statement)
      :summary "Gets the bank statement of a given account"
      (ok (get-bank-statement number)))))
