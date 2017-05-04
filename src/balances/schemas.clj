(ns balances.schemas
  (:require [schema.core :as s]))

(s/defschema Transaction
  {:id             Long
   :account-number s/Int
   :description    s/Str
   :amount         s/Num
   :date           s/Str})

(s/defschema NewTransaction
  (dissoc Transaction :id))

(s/defschema Account
  {:account-number s/Int
   :balance        s/Num})
