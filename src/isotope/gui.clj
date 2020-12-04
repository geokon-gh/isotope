(ns isotope.gui
  (:require [isotope.input :as input]
            [isotope.state :as state]
            [isotope.svg :as svg]
            [isotope.plot :as plot]
            [tick.alpha.api :as t]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [cljfx.api :as fx])
  (:import [javafx.scene.canvas Canvas]
           [javafx.scene.paint Color]))



(defn plotting-area
  [{:keys [fx/context
           width
           height]}]
  (let [oxygen (fx/sub context
                       state/oxygen)]
    (if (not-empty oxygen)
      {:fx/type :canvas
       :width width
       :height height
       :draw (fn [^Canvas canvas]
               (print \.)
               (doto (.getGraphicsContext2D canvas)
                 (.clearRect 0 0 width height)
                 (svg/paint-on-canvas (plot/plot-points width
                                                        height
                                                        oxygen
                                                        [(->> oxygen
                                                              (map first)
                                                              (apply min)
                                                              (+ (- 1)))
                                                         (->> oxygen
                                                              (map first)
                                                              (apply max)
                                                              (+ 1))]
                                                        [(->> oxygen
                                                              (map second)
                                                              (apply min)
                                                              (+ (- 1)))
                                                         (->> oxygen
                                                              (map second)
                                                              (apply max)
                                                              (+ 1))]))))}
      {:fx/type :text
       :text "No data to display"})))


(defn root
  "Takes the state atom (which is a map) and then get the mixers out of it and builds a windows with the mixers"
  [{:keys [fx/context]}]
  {:fx/type :stage
   ;; :icons [(-> (io/resource "128.png")
   ;;             .toString
   ;;             javafx.scene.image.Image.)]
   :title "Isotopes"
   :showing true
   :min-height 400
   :min-width 400
   :scene {:fx/type :scene
           :on-width-changed {:effect (fn [snapshot
                                           event]
                                        (fx/swap-context snapshot assoc :width (:fx/event event)))}
           :on-height-changed {:effect (fn [snapshot
                                            event]
                                         (fx/swap-context snapshot assoc :height (:fx/event event)))}
           :root {:fx/type :v-box
                  :children [{:fx/type :button
                              :text "Load Data!"
                              :on-action {:effect (fn [snapshot
                                                       event]
                                                    (fx/swap-context snapshot
                                                                     assoc
                                                                     :data
                                                                     (input/load-isotope-csv "/home/geokon/crabby.csv")))}}
                             {:fx/type plotting-area
                              :width (fx/sub context :width)
                              :height (fx/sub context :height)}]}}})
