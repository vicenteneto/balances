(ns balances.db
  (:require [clj-time.coerce :as c]
            [datomic.api :as d]))

(def uri "datomic:free://localhost:4334/balances")
(def conn (d/connect uri))

(defn by-id
  "Returns a transaction by its ID"
  [id]
  (first (d/q '[:find ?id ?account-number ?description ?amount ?date
                :in $ ?id
                :where
                [?id :transaction/account-number ?account-number]
                [?id :transaction/description ?description]
                [?id :transaction/amount ?amount]
                [?id :transaction/date ?date]]
              (d/db conn) id)))

(defn save
  "Saves a new transaction"
  [transaction]
  (second (:tx-data
            @(d/transact conn
                         [{:db/id                      (d/tempid :db.part/user)
                           :transaction/account-number (bigint (:account-number transaction))
                           :transaction/description    (:description transaction)
                           :transaction/amount         (bigdec (:amount transaction))
                           :transaction/date           (c/to-long (c/to-date-time (:date transaction)))}]))))

(defn list-all
  "Returns all saved transactions"
  []
  (d/q '[:find ?id ?account-number ?description ?amount ?date
         :where
         [?id :transaction/account-number ?account-number]
         [?id :transaction/description ?description]
         [?id :transaction/amount ?amount]
         [?id :transaction/date ?date]]
       (d/db conn)))

(defn list-by-account-number
  "Returns all transactions from a giving account number"
  [account-number]
  (d/q '[:find ?id ?account-number ?description ?amount ?date
         :in $ ?account-number
         :where
         [?id :transaction/account-number ?account-number]
         [?id :transaction/description ?description]
         [?id :transaction/amount ?amount]
         [?id :transaction/date ?date]]
       (d/db conn) account-number))
