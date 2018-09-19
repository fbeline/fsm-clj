(ns fsm-clj.core
  (:require [clojure.spec.alpha :as s]
            [dorothy.core :as dot]))

(s/def ::transition
  (s/or :t (s/and vector?
                  (s/cat
                   :state keyword?
                   :-> #(= % '->)
                   :target keyword?
                   :'when #(= % 'when)
                   :event keyword?
                   :'action #(= % 'action)
                   :action #(fn? @(-> % eval resolve))))
        :t (s/and vector?
                  (s/cat
                   :state keyword?
                   :-> #(= % '->)
                   :target keyword?
                   :'when #(= % 'when)
                   :event keyword?))))

(defn- validate-dsl! [transition]
  (if (s/valid? ::transition transition)
    transition
    (throw (Exception. (str "Invalid State Machine definition: " (s/explain-str ::transition transition))))))

(defn- parse-fsm-transition [transition]
  (let [parsed  (last (s/conform ::transition transition))
        action (:action parsed)]
    {:state   (:state parsed)
     :target  (:target parsed)
     :event   (:event parsed)
     :action (if (nil? action)
                (fn [acc _] acc)
                @(-> action eval resolve))}))

(defn- build-fsm-graph [transitions]
  (map (fn [{:keys [state target event]}]
         [state :> target {:label event}]) transitions))

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
  `(let [transitions# (map (comp #'parse-fsm-transition #'validate-dsl!) '~transitions)]
     {:state       (-> transitions# first :state)
      :transitions (#'build-fsm-transitions transitions#)
      :graph       (#'build-fsm-graph transitions#)}))

(defmacro defsm [name states]
  `(def ~name (fn tfsm#
                ([]
                 (tfsm# nil nil))
                ([acc#]
                 (tfsm# acc# nil))
                ([acc# initial-state#]
                 (-> (fsm ~states)
                     (assoc :value acc#)
                     (conj (when initial-state# [:state initial-state#])))))))

(defn send-event
  ([fsm event]
   (send-event fsm event nil))
  ([fsm event message]
   (let [state   (:state fsm)
         event   (-> fsm :transitions state event)
         handler (:action event)]
     (if event
       (-> fsm
           (update-in [:value] #(handler % message))
           (assoc :state (:target event)))
       fsm))))

(defn show! [fsm]
  (-> fsm :graph dot/digraph dot/dot dot/show!))
