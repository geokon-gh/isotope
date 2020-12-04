(ns isotope.state
  (:require
   [clojure.core.cache :as cache]
   [cljfx.api :as fx]))

(def *context
  ""
  (atom (fx/create-context {:data nil
                            :width 200
                            :height 200}
                           #(cache/lru-cache-factory % :threshold 4096))))

(defn data
  [context]
  (fx/sub context
          :data))

(defn days
  [context]
  (mapv :days
        (fx/sub context
                data)))

(defn oxygen
  [context]
  (let [days (fx/sub context
                     days)
        oxygen (mapv :d18O (fx/sub context
                                   data))]
    []
    (into [] (mapv (fn [day oxygen-count]
                     [day
                      (or oxygen-count
                          0)])
                   days
                   oxygen))))


