(ns isotope.gui
  (:require
   [isotope.input :as input]
   [isotope.state :as state]
   [isotope.svg :as svg]
   [isotope.plot :as plot]
   [tick.alpha.api :as t]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [cljfx.api :as fx])
  (:import
   [javafx.scene.canvas Canvas]
   [javafx.scene.transform Affine Translate]
   [javafx.scene.paint Color]))



(defn plotting-area
  [{:keys [fx/context
           width
           height]}]
  (let [oxygen (fx/sub context
                       state/oxygen)
        position-transform (fx/sub context
                                   :transform)]
    (if (not-empty oxygen)
      (let [oxygen-plot (plot/plot-points width
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
                                                              (+ 1))])]
      {:fx/type :canvas
       :width width
       :height height
       :on-mouse-dragged  {:effect (fn [snapshot
                                        {:keys [fx/event]}]
                                     (let [{:keys [last-x
                                                   last-y]} (fx/sub snapshot
                                                                    :mouse-drag)
                                           new-x (.getX event)
                                           new-y (.getY event)
                                           delta-x (- new-x
                                                      last-x)
                                           delta-y (- new-y
                                                      last-y)
                                           new-transform (Translate. delta-x
                                                                     delta-y)]
                                       (-> snapshot
                                           (fx/swap-context assoc-in
                                                            [:mouse-drag :last-x]
                                                            new-x)
                                           (fx/swap-context assoc-in
                                                            [:mouse-drag :last-y]
                                                            new-y)
                                           (fx/swap-context update
                                                            :transform
                                                            #(.createConcatenation % new-transform)))))}
       :on-mouse-pressed  {:effect (fn [snapshot
                                        {:keys [fx/event]}]
                                     (-> snapshot
                                         (fx/swap-context assoc-in
                                                          [:mouse-drag :last-x]
                                                          (.getX event))
                                         (fx/swap-context assoc-in
                                                          [:mouse-drag :last-y]
                                                          (.getY event))))}
       :on-mouse-released  {:effect (fn [snapshot
                                         {:keys [fx/event]}]
                                      (-> snapshot
                                          (fx/swap-context assoc
                                                           :mouse-drag
                                                           nil)))}
       :draw (fn [^Canvas canvas]
               (doto (.getGraphicsContext2D canvas)
                 (.clearRect (- width) (- height) (* 3 width) (* 3 height))
                 (.setTransform position-transform)
                 (svg/paint-on-canvas oxygen-plot)))})
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
