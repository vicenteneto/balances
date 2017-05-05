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
