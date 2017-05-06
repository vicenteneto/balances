(ns balances.db-test
  (:require [balances.db :refer :all]
            [balances.utils :refer :all]
            [balances.util.account-util :refer :all]
            [balances.util.db-util :refer :all]
            [midje.sweet :refer :all]))

(fact
  "Adding one transaction should allow us to find that transaction using the returned id"
  (with-redefs [conn (create-empty-in-memory-db)]
    (let [transaction (save-test-transaction-1)]
      (by-id (:id transaction)) => [(:id transaction) 123N "Deposit" 1000M (str-date-to-long (last test-transaction-1))])))
