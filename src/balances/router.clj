(ns balances.router
  (:require [balances.account :refer :all]
            [balances.schemas :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok not-found]]
            [schema.core :as s]))

(def api-routes
  (context "/api" []
    :tags ["api"]

    (POST "/transactions" []
      :return Transaction
      :body [transaction (describe NewTransaction "new transaction")]
      :summary "Saves a new transaction and return in"
      (ok (save-transaction transaction)))

    (GET "/transactions" []
      :return [Transaction]
      :summary "Retrieves all saved transactions"
      (ok (list-transactions)))

    (GET "/transactions/:id" []
      :path-params [id :- Long]
      :return (s/maybe Transaction)
      :summary "Returns a transaction by its ID"
      (let [transaction (find-transaction-by-id id)
            has-transaction (not (nil? transaction))
            response (if has-transaction (ok transaction) (not-found))]
        response))

    (GET "/account/:number/transactions" []
      :path-params [number :- s/Int]
      :return (s/maybe [Transaction])
      :summary "Returns all transactions from a giving account number"
      (let [transactions (list-transactions-by-account-number number)
            has-transactions (not (empty? transactions))
            response (if has-transactions (ok transactions) (not-found))]
        response))

    (GET "/account/:number/balance" []
      :path-params [number :- s/Int]
      :return (s/maybe Account)
      :summary "Returns the current balance from a giving account"
      (let [x (println 1 number)
            balance (get-balance number)
            has-balance (not (nil? balance))
            response (if has-balance (ok balance) (not-found))]
        response))

    (GET "/account/:number/statement" []
      :path-params [number :- s/Int]
      :return (s/maybe Statement)
      :summary "Returns the bank statement from a giving account"
      (let [statement (get-bank-statement number)
            has-statement (not (nil? statement))
            response (if has-statement (ok statement) (not-found))]
        response))

    (GET "/account/:number/debt-periods" []
      :path-params [number :- s/Int]
      :return (s/maybe [DebtPeriod])
      :summary "Returns the periods of debt from a giving account"
      (ok (get-debt-periods number)))))
