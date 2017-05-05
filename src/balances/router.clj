(ns balances.router
  (:require [balances.account :refer :all]
            [balances.schemas :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok]]
            [schema.core :as s]))

(def api-routes
  (context "/api" []
    :tags ["api"]

    (POST "/transactions" []
      :return Transaction
      :body [transaction (describe NewTransaction "new transaction")]
      :summary "Creates an transaction in the system"
      (ok (save-transaction transaction)))

    (GET "/transactions" []
      :return [Transaction]
      :summary "Gets all transactions"
      (ok (list-transactions)))

    (GET "/transactions/:id" []
      :path-params [id :- Long]
      :return (s/maybe Transaction)
      :summary "Gets all details relevant to a transaction"
      (ok (find-transaction-by-id id)))

    (GET "/account/:number/balance" []
      :path-params [number :- s/Int]
      :return (s/maybe Account)
      :summary "Gets the current balance from a giving account"
      (ok (get-balance number)))

    (GET "/account/:number/statement" []
      :path-params [number :- s/Int]
      :return (s/maybe Statement)
      :summary "Gets the bank statement of a given account"
      (ok (get-bank-statement number)))

    (GET "/account/:number/debt-periods" []
      :path-params [number :- s/Int]
      :return (s/maybe [DebtPeriod])
      :summary "Gets periods of debt of a given account"
      (ok (get-debt-periods number)))))
