(ns fsm-clj.parser
  (:require [clojure.spec.alpha :as s]))

(s/def ::action (s/cat :_ #{'action} :handler any?))
(s/def ::guard (s/cat :_ #{'guard} :handler any?))
(s/def ::transition
       (s/cat
        :state keyword?
        :_-> #{'->}
        :target keyword?
        :_when #{'when}
        :event keyword?
        :opts (s/* (s/alt :action ::action :guard ::guard))))

(defn- validate-dsl! [transition]
  (if (s/valid? ::transition transition)
    transition
    (throw (Exception. (str "Invalid State Machine definition: " (s/explain-str ::transition transition))))))

(defmacro eval-fn [opts attr]
  `(if-let [v# (->> ~opts (into {}) ~attr)]
     @(-> v# :handler eval resolve)
     (fn [acc# _#] (if (= ~attr :guard) true acc#))))

(defn- parse-fsm-transition [transition]
    (let [{:keys [state target event opts]} (s/conform ::transition transition)]
    {:state  state
     :target target
     :event  event
     :action (eval-fn opts :action)
     :guard  (eval-fn opts :guard)}))

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

