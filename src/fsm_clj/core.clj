(ns fsm-clj.core
  (:require [fsm-clj.parser :as p]
            [dorothy.core :as dorothy]))

(defn- set-state [fsm state]
  (if ((->> fsm :transitions keys (into #{})) state)
    (assoc fsm :state state)
    fsm))

(defmacro defsm
  "Define a State Machine.
  e.g.
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
                 (-> (p/fsm ~transitions)
                     (assoc :value acc#)
                     (#'set-state initial-state#))))))

(defn- on-transition-triggered [fsm transition message]
  (if (-> transition :guard (apply [(:value fsm) message]))
    (-> fsm
        (update-in [:value] #(-> transition :action (apply [% message])))
        (set-state (:target transition)))
    fsm))

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
   (let [state      (:state fsm)
         transition (-> fsm :transitions state event)]
     (if transition
       (on-transition-triggered fsm transition message)
       fsm))))

(defn show!
  "Graphically generate the State Machine (open a Swing viewer)."
  [fsm]
  (-> fsm :graph dorothy/digraph dorothy/dot dorothy/show!))
