(ns isotope.svg
  (:require [clojure.edn :as edn])
  (:import [javafx.scene.canvas GraphicsContext]
           [javafx.scene.paint Color]))

(defn set-attributes
  "Attributes (ie. the SVG figure's `setting`) are saved in a stack
  b/c child elements inherit parent attributes.
  The JavaFX Canvas is stateful and you can't undo attributes you've applied
  So each we apply the whole inherited attribute stack
  for each element before drawing it"
  [^GraphicsContext canvas
   attribute-stack]
  (mapv (fn [attributes]
          (if (some? attributes)
            (mapv #_(partial modify-canvas canvas)
                  (fn [attribute]
                    (let [key (first attribute)
                          value (if (= "none" (second attribute))
                                  "transparent"
                                  (second attribute))]
                      (case key
                        :stroke (.setStroke canvas
                                            (javafx.scene.paint.Paint/valueOf value))
                        :fill (.setFill canvas
                                        (javafx.scene.paint.Paint/valueOf value))
                        :font-family nil
                        :font-size nil
                        :text-anchor (.setTextAlign canvas
                                                    (case value
                                                      "middle" javafx.scene.text.TextAlignment/CENTER
                                                      "left" javafx.scene.text.TextAlignment/LEFT
                                                      "right" javafx.scene.text.TextAlignment/RIGHT
                                                      ))
                        :stroke-width (.setLineWidth canvas
                                                     value)
                        nil nil
                        nil)))
                  attributes)))
        attribute-stack))

(defn parse-points
  "Prolly could be written better with a reduce.."
  [points-str]
  (let [x-y-pairs (map #(let [[x-coord
                               y-coord](clojure.string/split %
                                                             #",")]
                          [(Double/parseDouble x-coord)
                           (Double/parseDouble y-coord)])
                       (clojure.string/split points-str #" "))]
    [(double-array (map first x-y-pairs))
     (double-array (map second x-y-pairs))]))

(defn paint-on-canvas
  ([^GraphicsContext canvas
    svg]
   (paint-on-canvas canvas
                    []
                    svg))
  ([^GraphicsContext canvas
   attribute-stack
    svg]
   (let [type (nth svg 0)]
     (if (coll? type)
       (mapv #(paint-on-canvas canvas
                               attribute-stack
                               %)
             svg)
       (let [new-attributes (nth svg 1)]
         (set-attributes canvas
                         (conj attribute-stack
                               new-attributes))
         ;;      (if (= type :text) (println "TEXT ATTRIBUTES: " attribute-stack))
         (case type
           (:g :svg) [{:fx/type :group
                       :children
                       (->> (rest (rest svg))
                            (mapv #(paint-on-canvas canvas
                                                    (conj attribute-stack
                                                          new-attributes)
                                                    %))
                            (apply concat)
                            (filterv some?))}]
           :line (.strokeLine canvas
                              (Double/parseDouble (:x1 new-attributes))
                              (Double/parseDouble (:y1 new-attributes))
                              (Double/parseDouble (:x2 new-attributes))
                              (Double/parseDouble (:y2 new-attributes)))
           :text (.strokeText canvas
                              ;; thing/geom is a bit weird here..
                              ;; the text for the `text` element is simply
                              ;; in the 3rd vector position
                              ;; not in an attribute key-value like others
                              (get svg 2)
                              (Double/parseDouble (:x new-attributes))
                              (Double/parseDouble (:y new-attributes)))
           :polyline (let [[x-s
                            y-s] (parse-points (:points new-attributes))]
                       (.strokePolyline canvas
                                        x-s
                                        y-s
                                        (count x-s)))
           :polygon  (let [[x-s
                            y-s] (parse-points (:points new-attributes))]
                       (.strokePolygon canvas
                                       x-s
                                       y-s
                                       (count x-s))
                       (.fillPolygon canvas
                                     x-s
                                     y-s
                                     (count x-s)))
           nil nil
           (println "Unexpected SVG element: " svg)))))))

;;  (isotope.plot/plot-points 100 100 [[0 1] [1 2] [2 4][3 2]] [0 5] [0 5])
