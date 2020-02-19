(ns blockchain.utils
  (:require [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(defn now []
  (-> (time/now)
      (coerce/to-long)
      (/ 1000)
      double
      int))

(defn hash-and-stringify [data]
  (-> (hash/sha3-256 data)
      codecs/bytes->hex))
