(ns balances.db
  (:require [datomic.api :as d]
            [clj-time.coerce :as c]
            [clj-time.format :as f]))

(def uri "datomic:free://localhost:4334/balances")
(def conn (d/connect uri))

(defn format-transaction
  ""
  [transaction & [date-as-str]]
  (let [date-as-str (or (nil? date-as-str) date-as-str)
        date (c/from-long (nth transaction 4))
        date (if date-as-str (str date) date)]
    {:id             (first transaction)
     :account-number (second transaction)
     :description    (nth transaction 2)
     :amount         (nth transaction 3)
     :date           date}))

(defn find-transaction-by-id
  ""
  [id & [date-as-str]]
  (let [res (first (d/q '[:find ?id ?account-number ?description ?amount ?date
                          :in $ ?id
                          :where
                          [?id :transaction/account-number ?account-number]
                          [?id :transaction/description ?description]
                          [?id :transaction/amount ?amount]
                          [?id :transaction/date ?date]]
                        (d/db conn) id))]
    (format-transaction res date-as-str)))

(defn save-transaction
  "Adds an transaction"
  [transaction]
  (let [res (second (:tx-data
                      @(d/transact conn
                                   [{:db/id                      (d/tempid :db.part/user)
                                     :transaction/account-number (bigint (:account-number transaction))
                                     :transaction/description    (:description transaction)
                                     :transaction/amount         (bigdec (:amount transaction))
                                     :transaction/date           (c/to-long (c/to-date-time (:date transaction)))}])))]
    (find-transaction-by-id (:e res) true)))

(defn list-transactions
  "Retrieves all transactions"
  []
  (let [res (d/q '[:find ?id ?account-number ?description ?amount ?date
                   :where
                   [?id :transaction/account-number ?account-number]
                   [?id :transaction/description ?description]
                   [?id :transaction/amount ?amount]
                   [?id :transaction/date ?date]]
                 (d/db conn))]
    (map #(format-transaction % true) res)))

(defn list-transactions-by-account-number
  "Retrieves all transactions from a giving account"
  [account-number]
  (let [res (d/q '[:find ?id ?account-number ?description ?amount ?date
                   :in $ ?account-number
                   :where
                   [?id :transaction/account-number ?account-number]
                   [?id :transaction/description ?description]
                   [?id :transaction/amount ?amount]
                   [?id :transaction/date ?date]]
                 (d/db conn) account-number)]
    (map #(format-transaction % false) res)))

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

(defn get-debt-periods
  "Retrieves periods of debt of a given account"
  [account-number]
  (let [db-transactions (d/q '[:find ?id ?account-number ?amount ?date
                               :in $ ?account-number
                               :where
                               [?id :transaction/account-number ?account-number]
                               [?id :transaction/amount ?amount]
                               [?id :transaction/date ?date]]
                             (d/db conn) account-number)
        grouped-transactions (group-by :key-date (map #(hash-map :id (first %)
                                                                 :account-number (second %)
                                                                 :amount (nth % 2)
                                                                 :date (str (c/from-long (nth % 3)))
                                                                 :key-date (f/unparse (f/formatters :year-month-day) (c/from-long (nth % 3)))) db-transactions))
        formatted-transactions (map #(hash-map (first %) {:transactions (map (fn [value] (dissoc value :key-date)) (second %)) :balance 0}) grouped-transactions)
        sorted-transactions (into (sorted-map) formatted-transactions)
        def-transactions (def transactions sorted-transactions)
        rdc-balance (reduce (fn
                              [& args]
                              (let [current-sum (apply + (map :amount (:transactions (second (second args)))))
                                    updated-sum (+ (second (first args)) current-sum)
                                    next-arg [(assoc (first (first args)) (first (second args)) updated-sum) updated-sum]
                                    def-transactions (def transactions (assoc-in transactions [(first (second args))] updated-sum))]
                                next-arg)) [{} 0] transactions)
        rdc-dates (reduce (fn
                            [& args]
                            (let [debts-list (first (first args))
                                  debt (second (first args))
                                  current-date (first (second args))
                                  current-balance (second (second args))
                                  contains-principal (contains? debt :principal)
                                  contains-start (contains? debt :start)
                                  contains-end (contains? debt :end)
                                  is-negative (< current-balance 0)
                                  is-principal (and is-negative (or (and contains-principal (< current-balance (:principal debt))) (not contains-principal)))
                                  new-debt (if is-principal (assoc debt :principal current-balance) debt)
                                  new-debt (if (and (not contains-start) is-negative) (assoc new-debt :start current-date) new-debt)
                                  new-debt (if (and contains-start (not contains-end) (not is-negative)) (assoc new-debt :end current-date) new-debt)
                                  create-new-debt (if (and (contains? new-debt :start) (contains? new-debt :end)) true false)
                                  next-arg (if create-new-debt [(concat debts-list [new-debt]) {}] [debts-list new-debt])]
                              next-arg)) [[] {}] transactions)
        rdc-dates (if (contains? (second rdc-dates) :start) [(concat (first rdc-dates) [(second rdc-dates)]) []] rdc-dates)]
    (first rdc-dates)))
