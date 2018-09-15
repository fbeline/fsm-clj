(ns fsm-clj.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::transition
  (s/and vector?
         (s/cat
           :state keyword?
           :-> #(= % '->)
           :target keyword?
           :'when #(= % 'when)
           :transition keyword?
           :'handler #(= % 'handler)
           :handler #(-> % eval fn?))))

(defn- parse-fsm-transition [transition]
  (when-not (s/valid? ::transition transition)
    (throw (Exception. (s/explain ::transition transition))))
  (let [parsed (s/conform ::transition transition)]
    {:state      (:state parsed)
     :target     (:target parsed)
     :transition (:transition parsed)
     :handler    (-> parsed :handler eval)}))

(defmacro fsm [transitions]
  `{:state       (-> '~transitions first parse-fsm-transition :state)
    :transitions (map parse-fsm-transition '~transitions)})

(defmacro defsm [name states]
  `(def ~name (fsm ~states)))

(defn send-event [fsm event message]
  (let [t (first (filter #(and (-> % :transition #{event}) (-> % :state #{(:state fsm)})) (:transitions fsm)))]
    (-> t :handler (apply [message]))
    (assoc fsm :state (or (:target t)
                          (:state fsm)))))

(defn force-state [fsm state]
  ;; check if state is valid
  (assoc fsm :state state))

(defsm foo
     [[:state-1 -> :state-2 when :event-1 handler #(println "state 1 boys" %)]
      [:state-2 -> :state-3 when :event-2 handler #(println "here we go boys!!" %)]])

(send-event foo :event-1 {})

;(defn payment [id value db]
;  (let [bill (db/get id)
;        fsm  (build-bill-fsm bill)])
;  (send-event fsm :payment {:id 10 :amount value}))
;
;(defn payment-handler [message db]
;  (db/save v db))
;
;(def cancel-handler [message db]
;  (db/cancel v db))
