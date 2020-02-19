(ns blockchain.transactions
  (:require [blockchain.utils :as utils]))


(defrecord TxOut [address
                  amount])

(defrecord TxIn [tx-out-id
                 tx-out-index
                 signature])

(defrecord Transaction [id
                        tx-ins
                        tx-outs])


(defn get-transaction-id [transaction]
  (let [tx-in-content (->> (:tx-ins transaction)
                           (reduce #(+ (:tx-out-id %2)
                                       (:tx-out-index %2)
                                       %1)))

        tx-out-content (->> (:tx-outs transaction)
                            (reduce 0 #(+ (:address %1)
                                          (:amount %1)
                                          %2)))]

    (utils/hash-and-stringify tx-in-content)))
