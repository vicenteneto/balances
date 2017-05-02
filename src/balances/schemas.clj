(ns balances.schemas
  (:require [schema.core :as s]))

(s/defschema Transaction
             {:id             Long
              :account-number s/Int
              :description    s/Str
              :amount         s/Num
              :date           s/Num})

(s/defschema NewTransaction
             (dissoc Transfer :id :date))
