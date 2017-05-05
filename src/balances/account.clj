(ns balances.account
  (:require [balances.db :as db]
            [balances.utils :as u]
            [clj-time.format :as f]))

(defn find-transaction-by-id
  ""
  [id & [date-as-str]]
  (let [res (db/by-id id)
        exists-transaction (not (nil? res))
        transaction (if exists-transaction (u/format-transaction res date-as-str))]
    transaction))

(defn save-transaction
  "Adds an transaction"
  [transaction]
  (let [res (db/save transaction)
        transaction (find-transaction-by-id (:e res) true)]
    transaction))

(defn list-transactions
  "Retrieves all transactions"
  []
  (let [res (db/list-all)
        transactions (map #(u/format-transaction % true) res)]
    transactions))

(defn list-transactions-by-account-number
  "Retrieves all transactions from a giving account"
  [account-number & [date-as-str]]
  (let [res (db/list-by-account-number account-number)
        transactions (map #(u/format-transaction % date-as-str) res)]
    transactions))

(defn get-balance
  "Gets the current balance from a giving account"
  [account-number]
  (let [res (db/sum-amount account-number)
        exists-transactions (not (nil? res))
        balance (if exists-transactions {:account-number account-number
                                         :balance        (if exists-transactions (second res) 0)})]
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
        transactions (reduce
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
        debts-periods (if (contains? (second transactions) :start) (into [] (concat (first transactions) [(second transactions)])) (first transactions))]
    debts-periods))
