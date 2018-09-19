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

(defn- set-state [fsm state]
  (if ((->> fsm :transitions keys (into #{})) state)
    (assoc fsm :state state)
    fsm))

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

(defmacro defsm
  "Define a State Machine.
  eg.
  (defsm foo
    [[:state1 -> :state2 when :event1]
     [:state2 -> state1 when :event2]])
  foo will be a function that when called start the fsm.
  By default, the start state will be the state of the first transition.

  You can start it in 3 different ways:
  Simplest form:
  (foo)
  With accumulator:
  (foo 0)
  Starting in a different state:
  (foo 0 :state2)"
  [name transitions]
  `(def ~name (fn tfsm#
                ([]
                 (tfsm# nil nil))
                ([acc#]
                 (tfsm# acc# nil))
                ([acc# initial-state#]
                 (-> (fsm ~transitions)
                     (assoc :value acc#)
                     (#'set-state initial-state#))))))

(defn send-event
  ([fsm event]
   "Send an event to a state machine.
   Parameters:
   - fsm: State machine
   - event: Event name. Must be a symbol."
   (send-event fsm event nil))
  ([fsm event message]
   "Send an event with a message to a state machine.
   Parameters:
   - fsm: State machine
   - event: Event name. Must be a symbol.
   - message: Message that will passed as argument to the action of the triggered transition."
   (let [state   (:state fsm)
         event   (-> fsm :transitions state event)
         handler (:action event)]
     (if event
       (-> fsm
           (update-in [:value] #(handler % message))
           (assoc :state (:target event)))
       fsm))))

(defn show!
  "Graphically generate the State Machine (open a Swing viewer)."
  [fsm]
  (-> fsm :graph dot/digraph dot/dot dot/show!))
