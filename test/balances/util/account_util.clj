(ns balances.util.account-util
  (:require [balances.account :refer :all]
            [clj-time.coerce :as c]))

(def test-transaction-1 [123 "Deposit" 1000 "2016-10-15T10:00:00.000Z"])
(def test-transaction-2 [123 "Purchase on Amazon" -3.34 "2016-10-16T08:00:00.000Z"])
(def test-transaction-3 [123 "Purchase on Uber" -45.23 "2016-10-16T12:00:00.000Z"])
(def test-transaction-4 [123 "Withdrawal" -180 "2016-10-16T12:00:00.000Z"])
(def test-transaction-5 [123 "Purchase of a flight ticket" -800 "2016-10-18T16:00:00.000Z"])
(def test-transaction-6 [123 "Deposit" 100 "2016-10-25T11:00:00.000Z"])

(defn save-test-transaction
  [account-number description amount date]
  (save-transaction {:account-number account-number
                     :description    description
                     :amount         amount
                     :date           date}))

(defn save-test-transaction-1 []
  (apply save-test-transaction test-transaction-1))

(defn save-test-transaction-2 []
  (apply save-test-transaction test-transaction-2))

(defn save-test-transaction-3 []
  (apply save-test-transaction test-transaction-3))

(defn save-test-transaction-4 []
  (apply save-test-transaction test-transaction-4))

(defn save-test-transaction-5 []
  (apply save-test-transaction test-transaction-5))

(defn save-test-transaction-6 []
  (apply save-test-transaction test-transaction-6))
