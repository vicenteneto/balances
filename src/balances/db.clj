(ns balances.db
  (:require [datomic.api :as d]
            [clj-time.coerce :as c]
            [clj-time.format :as f]))

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

(defn get-balance
  "Gets the current balance from a giving account"
  [account-number]
  (let [res (first (d/q '[:find ?account-number (sum ?amount) (sum ?id)
                          :in $ ?account-number
                          :where
                          [?id :transaction/account-number ?account-number]
                          [?id :transaction/amount ?amount]]
                        (d/db conn) account-number))]
    {:account-number (first res)
     :balance        (second res)}))

(defn get-bank-statement
  "Retrieves bank statement of a given account"
  [account-number]
  (let [db-transactions (d/q '[:find ?id ?account-number ?description ?amount ?date
                               :in $ ?account-number
                               :where
                               [?id :transaction/account-number ?account-number]
                               [?id :transaction/description ?description]
                               [?id :transaction/amount ?amount]
                               [?id :transaction/date ?date]]
                             (d/db conn) account-number)
        grouped-transactions (group-by :key-date (map #(hash-map :id (first %)
                                                                 :account-number (second %)
                                                                 :description (nth % 2)
                                                                 :amount (nth % 3)
                                                                 :date (str (c/from-long (nth % 4)))
                                                                 :key-date (f/unparse (f/formatters :year-month-day) (c/from-long (nth % 4)))) db-transactions))
        formatted-transactions (map #(hash-map (first %) {:transactions (map (fn [value] (dissoc value :key-date)) (second %)) :balance 0}) grouped-transactions)
        sorted-transactions (into (sorted-map) formatted-transactions)
        def-transactions (def transactions sorted-transactions)
        rdc (reduce (fn
                      [& args]
                      (let [current-sum (apply + (map :amount (:transactions (second (second args)))))
                            updated-sum (+ (second (first args)) current-sum)
                            next-arg [(assoc (first (first args)) (first (second args)) updated-sum) updated-sum]
                            def-transactions (def transactions (assoc-in transactions [(first (second args)) :balance] updated-sum))]
                        next-arg)) [{} 0] transactions)]
    transactions))
