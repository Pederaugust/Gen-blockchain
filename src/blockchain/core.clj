(ns blockchain.core
  (:require [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.string :as string]
            [clojure.core.async :refer [go <! chan close! put! alt!]]))

(defrecord Block
    ; O thou humble block of the chain
    [index hash previous-hash timestamp data difficulty nonce])

(defn now []
  (-> (time/now)
      (coerce/to-long)
      (/ 1000)
      double
      int))

(defn hash-and-stringify [data]
  (-> (hash/sha3-256 data)
      codecs/bytes->hex))

(def genesis-block
  ; And she gave birth to her firstborn, a son
  (map->Block {:index 0
               :hash (hash-and-stringify (str 0 "0" (now) "The Genesis block" 0 0))
               :previous-hash "0"
               :timestamp (now)
               :data "The Genesis block"
               :difficulty 1
               :nonce 0}))

(def block-chain
  ; And thus on the 7th day the chain was born, and it was good
  (atom [genesis-block]))

(defn get-latest-block! [] (last @block-chain))

(def difficulty-adjustment-interval 10)
(def block-generation-interval 5)


(defn calculate-hash [index previous-hash timestamp data difficulty nonce]
  (hash-and-stringify (str index previous-hash timestamp data difficulty nonce)))

(defn calculate-hash-from-block [{index :index previous-hash :previous-hash timestamp :timestamp data :data hash :hash difficulty :difficulty nonce :nonce}]
  (calculate-hash index previous-hash timestamp data difficulty nonce))


(defn valid-timestamp? [new-block previous-block]
  (and (< (- (:timestamp previous-block)
             60)
          (:timestamp new-block))
       (< (- (:timestamp new-block)
             60)
          (now))))

(defn valid-new-block? [new-block previous-block]
  (cond (not= (inc (:index previous-block))
              (:index new-block))
        (do (prn "Invalid Index")
            false)

        (not= (:hash previous-block)
              (:previous-hash new-block))
        (do (prn "Invalid previous hash")
            false)

        (not (valid-timestamp? new-block previous-block))
        (do (prn "Invalid Timestamp")
            false)

        (not= (calculate-hash-from-block new-block)
              (:hash new-block))
        (do (prn "Invalid hash: " (calculate-hash-from-block new-block)
                 " " (:hash new-block))
            false)

        :else true))


(defn valid-chain? [chain]
  (and (= (first chain) genesis-block)
       (loop [prev (first chain)
              lst (rest chain)]
         (cond (empty? lst)
               true

               (not (valid-new-block? (first lst) prev))
               false

               :else
               (recur (first lst) (rest lst))))))


(defn replace-chain! [new-blocks]
  (if (and (valid-chain? new-blocks)
           (> (count new-blocks) (count @block-chain)))
    (do (prn "Received blockchain is valid. Replacing current blockchain.")
        (reset! block-chain new-blocks))

    (do (prn "Received blockchain is invalid")
        false)))

(defn hash-matches-difficulty [hash difficulty]
  (->> (repeat difficulty "0")
       string/join
       (string/starts-with? hash)))

(defn get-adjusted-difficulty [latest-block chain]
  (let [prev-adjustment-block (nth chain
                                   (- (count chain)
                                      difficulty-adjustment-interval))

        time-expected (* block-generation-interval
                         difficulty-adjustment-interval)

        time-taken (- (:timestamp latest-block)
                      (:timestamp prev-adjustment-block))

        difficulty (:difficulty prev-adjustment-block)]

    (cond (< time-taken (/ time-expected 2))
          (inc difficulty)

          (> time-taken (* time-expected 2))
          (dec difficulty)

          :else
          difficulty)))


(defn get-difficulty [chain]
  (let [latest-block (last chain)]
    (if (and (zero? (mod (:index latest-block)
                         difficulty-adjustment-interval))
             (not (zero? (:index latest-block))))
      (get-adjusted-difficulty latest-block chain)
      (:difficulty latest-block))))

(defn mine-block [index previous-hash timestamp data difficulty]
  (loop [nonce 0]
    (let [hash (calculate-hash index
                               previous-hash
                               timestamp
                               data
                               difficulty
                               nonce)]

      (if (hash-matches-difficulty hash difficulty)
        (->Block index
                 hash
                 previous-hash
                 timestamp
                 data
                 difficulty
                 nonce)
        (recur (inc nonce))))))


(defn add-block! [new-block]
  (if (valid-new-block? new-block (get-latest-block!))
    ; And he was reborn a thousand times
    (swap! block-chain conj new-block)
    false))


(defn generate-next-block! [data chain]
  (let [prev (last chain)
        difficulty (get-difficulty chain)

        index (-> (:index prev)
                  inc)
        timestamp (now)

        block (mine-block index
                          (:hash prev)
                          timestamp
                          data
                          difficulty)]
                              
    (if (add-block! block)
      block
      false)))


;Example of mining to simulate the proof of work
(def run-mining-operation
  (let [stop (chan)]
    (go
      (loop []
        (when (alt! stop false :default :keep-going)
          (generate-next-block! "Some data" @block-chain)
          (recur))))
    stop))

; Shut down mining
; (put! run false)
; (close! my-chan)
