(ns isotope.plot
  (:require [thi.ng.geom.core :as g] ;; The graphing libraires
            [thi.ng.math.core :as m]
            [thi.ng.geom.viz.core :as viz]
            [thi.ng.geom.svg.core :as svg]))

(defn- grid-spec
  "Given a size (WIDTH HEIGHT) the output *spec* describes how the plot looks.
  More detail are in **geom-viz**.
  The data has been left initialized"
  [width
   height
   [min-x max-x]
   [min-y max-y]]
  {:x-axis (viz/linear-axis
            {:domain [min-x max-x]
             :range  [0.0 width]
             :pos    (* height
                        (/ max-y
                           (- max-y
                              min-y)))
             :visible true
             :label-style {:stroke "darkgray"
                           :fill "darkgray"
                           :font-family nil}
             :label-dist  (- height 1)
             ;;             :major-size 0
             :major 365
             :major-size -15
             :minor 31
             :attribs {:stroke "black"
                       :stroke-width 2.0} ;; axis line attributes
             })
   :y-axis (viz/linear-axis
            {:domain    [min-y max-y]
             :range       [height 0]
             ;; puts the axis out of view (can't show the grid with no axis)
             :pos  0.0 ;; major-size default
             :visible true
             :major 10 ;;(nearest-power-of-ten max-y)
             :label-dist -20
             :label-y 15
             :major-size -15
             :minor-size -15
             :label-style {:stroke "red"
                           :stroke-width 0.2
                           :fill "red"
                           :font-family nil}
             :attribs {:stroke "darkgray"
                       :stroke-width 1.5} ;; axis line attributes
             ;; :label-style {:fill "red" :text-anchor "start"}
             })
   :data []
   ;; :grid   {;:attribs {:stroke "#caa"}
   ;;          ; :minor-x true
   ;;          ; :major-x true
   ;;          ; :major-y true
   ;;          :minor-y true
   ;;          }
   })


(defn- add-lines
  "Add the lines to the graph"
  ([spec
    points]
   (add-lines spec
              points
              "black"))
  ([spec
    points
    color]
    (-> spec ;; do nothing.. else:
         (update :data
                 #(conj % {:values  points
                           :attribs {:fill "none" :stroke color :stroke-width 1.25}
                           :layout  viz/svg-line-plot})))))

(defn- add-points
  "Add the points (as little triangles) to the graph
  Also has an overload to supply your own point-shape function
  See `add-words` for an example"
  ([spec
    points]
   (add-points spec
               points
               (viz/svg-triangle-down 9)))
  ([spec
    points
    point-shape]
   (if (empty? points)
     spec ;; do nothing.. else:
     (assoc spec
            :data
            [{:values  points
              :attribs {:fill "none" :stroke "slategray"}
              :shape point-shape
              :layout  viz/svg-scatter-plot}
             ]))))

(defn- add-words
  "Overload for `add-points` to directly display a word/letter at plot point
  See `geom-viz` for details, but the shape supplied is an svg function"
  [spec
   points
   word]
  (add-points spec
              points
              (fn [[[x y]]] (svg/text [x y] word))))

(defn- add-red-overlay
  "Will draw red overlays at the selected points
  ie. the points selected to be cropped"
  [spec
   points]
  (if (empty? points)
    spec
    (update spec
            :data
            #(into %
                   [{:values  points
                     :attribs {:fill "pink" :stroke "red" :stroke-width 1.00}
                     ;;                    :bar-width 100
                     :interleave 1
                     :layout  viz/svg-bar-plot}]))))

(defn- add-seam-marker
  "Will draw red overlays at the selected points
  ie. the points selected to be cropped"
  [spec
   seams
   plot-height]
  (if (empty? seams)
    spec
    (update spec
            :data
            #(into %
                   [{:values (into [] (map (fn [pos] (vector pos
                                                             plot-height))
                                           seams))
                     :attribs {:fill "#bfbfbf" :stroke "dimgray" :stroke-dasharray "1 5"}
                     ;;                    :bar-width 100
                     :interleave 1
                     :layout  viz/svg-bar-plot}]))))

(defn plot-points
  ""
  [width
   height
   points
   [min-x max-x]
   [min-y max-y]]
(-> (grid-spec width
               height
               [min-x
                max-x]
               [ min-y
                 max-y])
    (add-points points)
    (viz/svg-plot2d-cartesian)
    ))

;; (isotope.plot/plot-points 100 100 [[0 1] [1 2] [2 4][3 2]] [0 5] [0 5])

  #_(defn plot-points
  ""
  [width
   height
   points
   [min-x max-x]
   [min-y max-y]
   ;; crop-left
   ;; crop-right
   seams
   lines?]
  (let [
        right-crop-points (if (zero? crop-right)
                            []
                            (filter #(> (first %)
                                        (- max-x
                                           crop-right))
                                    points))
        left-crop-points (if (zero? crop-left)
                           []
                           (filter #(< (first %)
                                     crop-left)
                                 points))
        crop-points (concat right-crop-points
                            left-crop-points)
        graph-height (nice-max-count max-y)]
    (cond-> (grid-spec width
                       height
                       [min-x
                        max-x]
                       [(nice-max-count min-y)
                        (nice-max-count max-y)])
      lines? (add-lines points)
      (not lines?)  (add-points points)
      (seq crop-points) (add-red-overlay crop-points)
      (seq seams) (add-seam-marker seams
                            graph-height)
      true (viz/svg-plot2d-cartesian))))

