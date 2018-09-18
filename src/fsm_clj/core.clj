(ns fsm-clj.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::transition
  (s/or :t (s/and vector?
                  (s/cat
                   :state keyword?
                   :-> #(= % '->)
                   :target keyword?
                   :'when #(= % 'when)
                   :event keyword?
                   :'handler #(= % 'handler)
                   :handler #(fn? @(-> % eval resolve))))
        :t (s/and vector?
                  (s/cat
                   :state keyword?
                   :-> #(= % '->)
                   :target keyword?
                   :'when #(= % 'when)
                   :event keyword?))))

(defn parse-fsm-transition [transition]
  (let [parsed  (last (s/conform ::transition transition))
        handler (:handler parsed)]
    {:state   (:state parsed)
     :target  (:target parsed)
     :event   (:event parsed)
     :handler (if (nil? handler)
                (fn [acc _] acc)
                @(-> handler eval resolve))}))

(defn build-fsm-graph [transitions]
  (group-by :state transitions))

(defn build-fsm-transitions [transitions]
  (->> transitions
       (group-by :state)
       (map (fn [[k v]]
              {k (->> v
                      (group-by :event)
                      (map (fn [[k v]] {k (first v)}))
                      (into {}))}))
       (into {})))

(defmacro fsm [transitions]
  `(let [transitions# (map parse-fsm-transition '~transitions)]
     {:state       (-> transitions# first :state)
      :transitions (build-fsm-transitions transitions#)
      :graph       (build-fsm-graph transitions#)}))

(defmacro defsm [name states]
  `(def ~name (fn tfsm#
                ([acc#]
                 (tfsm# acc# nil))
                ([acc# initial-state#]
                 (-> (fsm ~states)
                     (assoc :acc acc#)
                     (conj (when initial-state# [:state initial-state#])))))))

(defn send-event
  ([fsm event]
   (send-event fsm event nil))
  ([fsm event message]
   (let [state   (:state fsm)
         event   (-> fsm :transitions state event)
         handler (:handler event)]
     (if event
       (-> fsm
           (update-in [:acc] #(handler % message))
           (assoc :state (:target event)))
       fsm))))
