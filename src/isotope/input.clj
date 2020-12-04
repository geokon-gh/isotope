(ns isotope.input
  (:require [tick.alpha.api :as t]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn load-isotope-csv
  "Note: XRF files are tab separated CSV files
  with an extrenious tab at the end
  Hence the `butlast` in there.."
  [csv-file]
  (let [file (io/file csv-file)
        full-csv-table (-> file
                           (.getCanonicalPath)
                           (io/reader)
                           (csv/read-csv :separator \,))
        header (->> (first full-csv-table)
                    (map #(clojure.string/replace % #"[() ]" "-"))
                    (map keyword))
        first-date (t/date (first (second full-csv-table)))
        data-map (->> (rest full-csv-table)
                      (map #(zipmap header %)) ; put data into a map
                      (map #(-> % ; convert data fields from strings to integers/dates
                                (update :Date t/date)
                                (update :Rain--mm- edn/read-string)
                                (update :d18O edn/read-string)
                                (update :dD edn/read-string)
                                identity))
                      ;; https://github.com/juxt/tick/issues/126
                      ;; https://docs.oracle.com/javase/tutorial/datetime/iso/overview.html
                      (map #(assoc % ; make a field for the number of days since-start
                                   :days
                                   (-> (t/new-interval first-date
                                                       (:Date %))
                                       t/duration
                                       t/days
                                       dec))))]
    data-map))

;;(def crab (load-isotope-csv "/home/geokon/crabby.csv"))
