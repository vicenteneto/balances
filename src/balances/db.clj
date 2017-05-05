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
  [account-number & [date-as-str]]
  (let [res (d/q '[:find ?id ?account-number ?description ?amount ?date
                   :in $ ?account-number
                   :where
                   [?id :transaction/account-number ?account-number]
                   [?id :transaction/description ?description]
                   [?id :transaction/amount ?amount]
                   [?id :transaction/date ?date]]
                 (d/db conn) account-number)]
    (map #(format-transaction % date-as-str) res)))

(defn get-balance
  "Gets the current balance from a giving account"
  [account-number]
  (let [res (first (d/q '[:find ?account-number (sum ?amount) (sum ?id)
                          :in $ ?account-number
                          :where
                          [?id :transaction/account-number ?account-number]
                          [?id :transaction/amount ?amount]]
                        (d/db conn) account-number))
        exists-transactions (not (nil? res))
        balance {:account-number account-number
                 :balance        (if exists-transactions (second res) 0)}]
    balance))

(defn get-bank-statement
  "Retrieves bank statement of a given account"
  [account-number]
  (let [transactions (list-transactions-by-account-number account-number false)
        transactions (map #(assoc % :key-date (f/unparse (f/formatters :year-month-day) (:date %))) transactions)
        transactions (map #(assoc % :date (str (:date %))) transactions)
        transactions (group-by :key-date transactions)
        transactions (map #(hash-map (first %) (map (fn [value] (dissoc value :key-date)) (second %))) transactions)
        transactions (into (sorted-map) transactions)
        transactions (first
                       (reduce
                         (fn [& args]
                           (let [transactions (first (first args))
                                 total-balance (second (first args))
                                 date (first (second args))
                                 date-transactions (second (second args))
                                 date-balance (apply + (map :amount date-transactions))
                                 total-balance (+ total-balance date-balance)
                                 transactions (assoc transactions date {:transactions date-transactions
                                                                        :balance      total-balance})]
                             [transactions total-balance]))
                         [{} 0] transactions))]
    transactions))

(defn get-debt-periods
  "Retrieves periods of debt of a given account"
  [account-number]
  (let [transactions (get-bank-statement account-number)
        rdc-transactions (reduce
                           (fn [& args]
                             (let [debts-periods (first (first args))
                                   debt (second (first args))
                                   date (first (second args))
                                   balance (:balance (second (second args)))
                                   contains-principal (contains? debt :principal)
                                   contains-start (contains? debt :start)
                                   contains-end (contains? debt :end)
                                   is-negative (< balance 0)
                                   is-principal (and is-negative (or (and contains-principal (< balance (:principal debt))) (not contains-principal)))
                                   debt (if is-principal (assoc debt :principal balance) debt)
                                   debt (if (and (not contains-start) is-negative) (assoc debt :start date) debt)
                                   debt (if (and contains-start (not contains-end) (not is-negative)) (assoc debt :end date) debt)
                                   create-new-debt (if (and (contains? debt :start) (contains? debt :end)) true false)
                                   debts-periods (if create-new-debt (into [] (concat debts-periods [debt])) debts-periods)
                                   debt (if create-new-debt {} debt)]
                               [debts-periods debt])) [[] {}] transactions)
        transactions (if (contains? (second rdc-transactions) :start) (into [] (concat (first rdc-transactions) [(second rdc-transactions)])) (first rdc-transactions))]
    transactions))
