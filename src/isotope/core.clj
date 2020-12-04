(ns isotope.core
  (:require
   [isotope.gui :as gui]
   [isotope.state :as state]
   [cljfx.api :as fx]
   [cljfx.ext.list-view :as fx.ext.list-view])
  (:import javafx.scene.image.Image
           javafx.stage.DirectoryChooser
           javafx.stage.FileChooser
           javafx.application.Platform
           javafx.stage.Stage)
  (:gen-class :main true))

(set! *warn-on-reflection* true)

(defn event-handler-wrapper
  [{:keys [snapshot
           effect] :as event}]
  {:updated-context (effect snapshot
                            (dissoc event
                                    :effect
                                    :snapshot))})

(def event-dispatcher
  (-> event-handler-wrapper
      ;; adds the current state to every processed event
      ;; the event handler can then operate on the current state
      ;; and doesn't need to do it own dereferencing
      (fx/wrap-co-effects {:snapshot #(deref state/*context)})
      ;; wrap-effects will take:
      ;; - a key where it will some data
      ;; - a side-effect function of what to do with the data
      ;; in our case the data will be an updated state
      ;; and it will update the global state with this updated state
      (fx/wrap-effects {:updated-context (fn [our-updated-context _]
                                           (reset! isotope.state/*context ;; feel this should be a `reset-context`
                                                   our-updated-context))})))

(def renderer
  (fx/create-renderer
   :middleware (comp
                ;; passes the state context to all lifecycles
                fx/wrap-context-desc
                (fx/wrap-map-desc (fn [_] {:fx/type gui/root})))
   :opts {:fx.opt/map-event-handler event-dispatcher
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       ;; For functions in `:fx/type` values, pass
                                       ;; context from option map to these functions
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& args]
  ;; This make the application exit when you close all the windows
  ;; Obviously problematic if you're doing stuff in the REPL and want to close and relaunch...
  ;; (Platform/setImplicitExit true)
  (fx/mount-renderer
   isotope.state/*context
   renderer))
