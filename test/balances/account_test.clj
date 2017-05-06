(ns balances.account-test
  (:require [balances.account :refer :all]
            [balances.db :refer :all]
            [balances.util.account-util :refer :all]
            [balances.util.db-util :refer :all]
            [datomic.api :as d]
            [midje.sweet :refer :all]))

;; ----- Transaction tests -----

(fact "Adding one transaction should allow us to find that transaction using the returned id"
      (with-redefs [conn (create-empty-in-memory-db)]
        (let [transaction (save-test-transaction-1)]
          (find-transaction-by-id (:id transaction)) => {:id             (:id transaction)
                                                         :account-number 123N
                                                         :description    "Deposit"
                                                         :amount         1000M
                                                         :date           "2016-10-15T10:00:00.000Z"})))

(fact "Adding multiple transactions should allow us to find all those transactions"
      (with-redefs [conn (create-empty-in-memory-db)]
        (let [transaction-1 (save-test-transaction-1)
              transaction-2 (save-test-transaction-2)]
          (list-transactions) => [{:id             (:id transaction-1)
                                   :account-number 123N
                                   :description    "Deposit"
                                   :amount         1000M
                                   :date           "2016-10-15T10:00:00.000Z"}
                                  {:id             (:id transaction-2)
                                   :account-number 123N
                                   :description    "Purchase on Amazon"
                                   :amount         -3.34M
                                   :date           "2016-10-16T08:00:00.000Z"}])))

(fact "Adding multiple transactions should allow us to get the current balance from a giving account"
      (with-redefs [conn (create-empty-in-memory-db)]
        (let [transaction-1 (save-test-transaction-1)
              transaction-2 (save-test-transaction-2)]
          (get-balance 123) => {:account-number 123
                                :balance        996.66M})))
