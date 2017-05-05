(ns balances.db
  (:require [clj-time.coerce :as c]
            [datomic.api :as d]))

(def uri "datomic:free://localhost:4334/balances")
(def conn (d/connect uri))

(defn by-id
  ""
  [id & [date-as-str]]
  (first (d/q '[:find ?id ?account-number ?description ?amount ?date
                :in $ ?id
                :where
                [?id :transaction/account-number ?account-number]
                [?id :transaction/description ?description]
                [?id :transaction/amount ?amount]
                [?id :transaction/date ?date]]
              (d/db conn) id)))

(defn save
  "Adds an transaction"
  [transaction]
  (second (:tx-data
            @(d/transact conn
                         [{:db/id                      (d/tempid :db.part/user)
                           :transaction/account-number (bigint (:account-number transaction))
                           :transaction/description    (:description transaction)
                           :transaction/amount         (bigdec (:amount transaction))
                           :transaction/date           (c/to-long (c/to-date-time (:date transaction)))}]))))

(defn list-all
  "Retrieves all transactions"
  []
  (d/q '[:find ?id ?account-number ?description ?amount ?date
         :where
         [?id :transaction/account-number ?account-number]
         [?id :transaction/description ?description]
         [?id :transaction/amount ?amount]
         [?id :transaction/date ?date]]
       (d/db conn)))

(defn list-by-account-number
  "Retrieves all transactions from a giving account"
  [account-number & [date-as-str]]
  (d/q '[:find ?id ?account-number ?description ?amount ?date
         :in $ ?account-number
         :where
         [?id :transaction/account-number ?account-number]
         [?id :transaction/description ?description]
         [?id :transaction/amount ?amount]
         [?id :transaction/date ?date]]
       (d/db conn) account-number))

(defn sum-amount
  "Gets the current balance from a giving account"
  [account-number]
  (first (d/q '[:find ?account-number (sum ?amount) (sum ?id)
                :in $ ?account-number
                :where
                [?id :transaction/account-number ?account-number]
                [?id :transaction/amount ?amount]]
              (d/db conn) account-number)))

