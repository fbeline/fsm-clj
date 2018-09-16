(ns fsm-clj.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::transition
  (s/and vector?
         (s/cat
           :state keyword?
           :-> #(= % '->)
           :target keyword?
           :'when #(= % 'when)
           :event keyword?
           :'handler #(= % 'handler)
           :handler #(-> % eval fn?))))

(defn- parse-fsm-transition [transition]
  (when-not (s/valid? ::transition transition)
    (throw (Exception. (s/explain ::transition transition))))
  (let [parsed (s/conform ::transition transition)]
    {:state      (:state parsed)
     :target     (:target parsed)
     :event      (:event parsed)
     :handler    (-> parsed :handler eval (or identity))}))

(defn- build-fsm-graph [transitions]
  (group-by :state transitions))

(defn- build-fsm-transitions [transitions]
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
  `(def ~name (fsm ~states)))

(defn send-event [fsm event message]
  (let [state (:state fsm)
        event (-> fsm :transitions state event)]
    (if event
      (do
        (-> event :handler (apply [message]))
        (assoc fsm :state (:target event)))
      fsm)))

(defn force-state [fsm state]
  ;; check if state is valid
  (assoc fsm :state state))

(defsm foo
     [[:state-1 -> :state-2 when :event-1 handler #(println "state 1 boys" %)]
      [:state-1 -> :state-10 when :end handler #(println "final state" %)]
      [:state-2 -> :state-3 when :event-2 handler identity]])

(-> foo
    (send-event :end "nicee"))
