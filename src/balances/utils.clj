(ns balances.utils
  (:require [clj-time.coerce :as c]))

(defn str-date-to-long
  [str-date]
  (c/to-long (c/to-date-time str-date)))

(defn format-transaction
  "Formats a database transaction to the schema format and return it"
  [transaction & [date-as-str]]
  (let [date-as-str (or (nil? date-as-str) date-as-str)
        date (c/from-long (nth transaction 4))
        date (if date-as-str (str date) date)]
    {:id             (first transaction)
     :account-number (second transaction)
     :description    (nth transaction 2)
     :amount         (nth transaction 3)
     :date           date}))
