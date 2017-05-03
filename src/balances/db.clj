(ns balances.db
  (:require [datomic.api :as d]
            [clj-time.coerce :as c]))

(def uri "datomic:free://localhost:4334/balances")
(def conn (d/connect uri))

(defn get-transaction [id]
  (let [res (first (d/q '[:find ?id ?account-number ?description ?amount ?date
                          :in $ ?id
                          :where
                          [?id :transaction/account-number ?account-number]
                          [?id :transaction/description ?description]
                          [?id :transaction/amount ?amount]
                          [?id :transaction/date ?date]]
                        (d/db conn) id))
        description (nth res 2)
        amount (nth res 3)
        date (str (c/from-long (nth res 4)))]
    {:id             (first res)
     :account-number (second res)
     :description    description
     :amount         amount
     :date           date}))

(defn add-transaction
  "Adds an transaction"
  [transaction]
  (let [account-number (bigint (:account-number transaction))
        description (:description transaction)
        amount (bigdec (:amount transaction))
        date (c/to-long (c/to-date-time (:date transaction)))
        res (second (:tx-data
                      @(d/transact conn
                                   [{:db/id                      (d/tempid :db.part/user)
                                     :transaction/account-number account-number
                                     :transaction/description    description
                                     :transaction/amount         amount
                                     :transaction/date           date}])))]
    (get-transaction (:e res))))

(defn get-transactions
  "Retrieves all transactions"
  []
  (let [res (d/q '[:find ?id ?account-number ?description ?amount ?date
                   :where
                   [?id :transaction/account-number ?account-number]
                   [?id :transaction/description ?description]
                   [?id :transaction/amount ?amount]
                   [?id :transaction/date ?date]]
                 (d/db conn))]
    (map #(hash-map :id (first %)
                    :account-number (second %)
                    :description (nth % 2)
                    :amount (nth % 3)
                    :date (str (c/from-long (nth % 4)))) res)))
