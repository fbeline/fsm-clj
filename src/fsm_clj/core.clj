(ns fsm-clj.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::state
  (s/and vector?
         (s/cat
           :state keyword?
           :-> #(= % '->)
           :target keyword?
           :transition-> #(= % 'transition->)
           :transition keyword?)))

(defn- parse-fsm-state [state]
  (when-not (s/valid? ::state state)
    (throw (Exception. "Invalid fsm definition")))
  (let [parsed (s/conform ::state state)]
    {:state      (:state parsed)
     :target     (:target parsed)
     :transition (:transition parsed)}))

(defmacro fsm [states]
  `{:current (-> '~states first parse-fsm-state :state)
    :states  (map parse-fsm-state '~states)})

(defmacro defsm [name states]
  `(def ~name (fsm ~states)))

(defn send-event [fsm event]
  (assoc fsm :current (or (:target (first (filter #(and (-> % :transition #{event})
                                                        (-> % :state #{(:current fsm)})) (:states fsm))))
                          (:current fsm))))

(defsm foo
     [[:state-1 -> :state-2 transition-> :event-1]
      [:state-2 -> :state-3 transition-> :event-2]])
