(ns balances.db-test
  (:require [balances.db :refer :all]
            [datomic.api :as d]
            [midje.sweet :refer :all]))

;; ----- Database connection for tests -----

(defn create-empty-in-memory-db []
  (let [uri "datomic:mem://balances-test-db"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)
          schema (read-string (slurp "resources/schema.dtm"))]
      (d/transact conn schema)
      conn)))

;; ----- Transaction tests -----

(fact "Adding one transaction should allow us to find that transaction using the returned id"
      (with-redefs [conn (create-empty-in-memory-db)]
        (let [transaction (add-transaction {:account-number 123
                                            :description    "Deposit 1000.00 at 15/10"
                                            :amount         1000
                                            :date           "2016-10-15T10:30:45.000Z"})]
          (get-transaction (:id transaction)) => {:id             (:id transaction)
                                                  :account-number 123N
                                                  :description    "Deposit 1000.00 at 15/10"
                                                  :amount         1000M
                                                  :date           "2016-10-15T10:30:45.000Z"})))

(fact "Adding multiple transactions should allow us to find all those transactions"
      (with-redefs [conn (create-empty-in-memory-db)]
        (let [transaction-1 (add-transaction {:account-number 123
                                              :description    "Deposit 1000.00 at 15/10"
                                              :amount         1000
                                              :date           "2016-10-15T10:30:45.000Z"})
              transaction-2 (add-transaction {:account-number 123
                                              :description    "Purchase on Amazon 3.34 at 16/10"
                                              :amount         -3.34
                                              :date           "2016-10-16T08:00:00.000Z"})
              transaction-3 (add-transaction {:account-number 456
                                              :description    "Purchase on Uber 45.23 at 16/10"
                                              :amount         -45.23
                                              :date           "2016-10-16T12:00:00.000Z"})]
          (get-transactions) => [{:id             (:id transaction-2)
                                  :account-number 123N
                                  :description    "Purchase on Amazon 3.34 at 16/10"
                                  :amount         -3.34M
                                  :date           "2016-10-16T08:00:00.000Z"}
                                 {:id             (:id transaction-1)
                                  :account-number 123N
                                  :description    "Deposit 1000.00 at 15/10"
                                  :amount         1000M
                                  :date           "2016-10-15T10:30:45.000Z"}
                                 {:id             (:id transaction-3)
                                  :account-number 456N
                                  :description    "Purchase on Uber 45.23 at 16/10"
                                  :amount         -45.23M
                                  :date           "2016-10-16T12:00:00.000Z"}])))

(fact "Adding multiple transactions should allow us to get the current balance from a giving account"
      (with-redefs [conn (create-empty-in-memory-db)]
        (let [transaction-1 (add-transaction {:account-number 123
                                              :description    "Deposit 1000.00 at 15/10"
                                              :amount         1000
                                              :date           "2016-10-15T10:30:45.000Z"})
              transaction-2 (add-transaction {:account-number 123
                                              :description    "Purchase on Amazon 50 at 16/10"
                                              :amount         -50
                                              :date           "2016-10-16T08:00:00.000Z"})]
          (get-balance 123) => {:account-number 123N
                                :balance        950M})))


(fact "Adding multiple transactions should allow us to get the bank statement from a specific account"
      (with-redefs [conn (create-empty-in-memory-db)]
        (let [transaction-1 (add-transaction {:account-number 123
                                              :description    "Deposit 1000.00 at 15/10"
                                              :amount         1000
                                              :date           "2016-10-15T10:30:45.000Z"})
              transaction-2 (add-transaction {:account-number 123
                                              :description    "Purchase on Amazon 50 at 16/10"
                                              :amount         -50
                                              :date           "2016-10-16T08:00:00.000Z"})
              transaction-3 (add-transaction {:account-number 456
                                              :description    "Purchase on Uber 45.23 at 16/10"
                                              :amount         -45.23
                                              :date           "2016-10-16T12:00:00.000Z"})]
          (get-bank-statement 123) => {"2016-10-15" {:transactions [{:id             (:id transaction-1)
                                                                     :account-number 123N
                                                                     :description    "Deposit 1000.00 at 15/10"
                                                                     :amount         1000M
                                                                     :date           "2016-10-15T10:30:45.000Z"}]
                                                     :balance      1000M}
                                       "2016-10-16" {:transactions [{:id             (:id transaction-2)
                                                                     :account-number 123N
                                                                     :description    "Purchase on Amazon 50 at 16/10"
                                                                     :amount         -50M
                                                                     :date           "2016-10-16T08:00:00.000Z"}]
                                                     :balance      -50M}})))
