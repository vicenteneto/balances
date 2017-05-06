(ns balances.util.db-util
  (:require [balances.db :refer :all]
            [datomic.api :as d]))

;; ----- Database connection for tests -----
(defn create-empty-in-memory-db []
  (let [uri "datomic:mem://balances-test-db"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)
          schema (read-string (slurp "resources/schema.dtm"))]
      (d/transact conn schema)
      conn)))
