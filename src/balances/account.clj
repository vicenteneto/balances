(ns balances.account
  (:require [balances.db :as db]
            [balances.utils :as u]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(defn find-transaction-by-id
  "Returns a transaction by its ID"
  [id & [date-as-str]]
  (let [res (db/by-id id)
        exists-transaction (not (nil? res))
        transaction (if exists-transaction (u/format-transaction res date-as-str))]
    transaction))

(defn save-transaction
  "Saves a new transaction and return it"
  [transaction]
  (let [res (db/save transaction)
        transaction (find-transaction-by-id (:e res) true)]
    transaction))

(defn list-transactions
  "Returns all saved transactions"
  []
  (let [res (db/list-all)
        transactions (into [] (map #(u/format-transaction % true) res))]
    transactions))

(defn list-transactions-by-account-number
  "Returns all transactions from a giving account number"
  [account-number & [date-as-str]]
  (let [res (db/list-by-account-number account-number)
        transactions (map #(u/format-transaction % date-as-str) res)]
    transactions))

(defn get-balance
  "Returns the current balance from a giving account"
  [account-number]
  (let [transactions (list-transactions-by-account-number account-number)
        balance (apply + (map :amount transactions))
        exists-transactions (not (empty? transactions))
        balance (if exists-transactions {:account-number account-number
                                         :balance        balance})]
    balance))

(defn format-bank-statement [previous-response current-item]
  (let [transactions (first previous-response)
        total-balance (second previous-response)
        date (first current-item)
        date-transactions (second current-item)
        date-balance (apply + (map :amount date-transactions))
        total-balance (+ total-balance date-balance)
        transactions (assoc transactions date {:transactions date-transactions
                                               :balance      total-balance})]
    [transactions total-balance]))

(defn get-bank-statement
  "Returns the bank statement from a giving account"
  [account-number]
  (let [transactions (list-transactions-by-account-number account-number false)
        transactions (map #(assoc % :key-date (f/unparse (f/formatters :year-month-day) (:date %))) transactions)
        transactions (into [] (map #(assoc % :date (str (:date %))) transactions))
        bank-statement (group-by :key-date transactions)
        bank-statement (map #(hash-map (first %) (map (fn [value] (dissoc value :key-date)) (second %))) bank-statement)
        bank-statement (into (sorted-map) bank-statement)
        bank-statement (first (reduce (fn [& args] (format-bank-statement (first args) (second args))) [{} 0] bank-statement))
        bank-statement (if (not (empty? bank-statement)) bank-statement)]
    bank-statement))

(defn format-debt-periods [previous-response current-item]
  (let [debts-periods (first previous-response)
        debt (second previous-response)
        date (first current-item)
        balance (:balance (second current-item))
        contains-principal (contains? debt :principal)
        contains-start (contains? debt :start)
        contains-end (contains? debt :end)
        is-negative (< balance 0)
        is-principal (and is-negative (or (and contains-principal (< balance (:principal debt))) (not contains-principal)))
        end-date (f/unparse (f/formatters :year-month-day) (t/plus (f/parse (f/formatter :year-month-day) date) (t/days 3)))
        debt (if is-principal (assoc debt :principal balance) debt)
        debt (if (and (not contains-start) is-negative) (assoc debt :start date) debt)
        debt (if (and contains-start (not contains-end) (not is-negative)) (assoc debt :end end-date) debt)
        create-new-debt (if (and (contains? debt :start) (contains? debt :end)) true false)
        debts-periods (if create-new-debt (into [] (concat debts-periods [debt])) debts-periods)
        debt (if create-new-debt {} debt)]
    [debts-periods debt]))

(defn get-debt-periods
  "Returns the periods of debt from a giving account"
  [account-number]
  (let [transactions (get-bank-statement account-number)
        transactions (reduce (fn [& args] (format-debt-periods (first args) (second args))) [[] {}] transactions)
        debts-periods (if (contains? (second transactions) :start) (into [] (concat (first transactions) [(second transactions)])) (first transactions))]
    debts-periods))
