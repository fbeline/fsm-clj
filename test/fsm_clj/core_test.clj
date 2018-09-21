(ns fsm-clj.core-test
  (:require [clojure.test :refer :all]
            [fsm-clj.core :as fsm]))

(defn inc-handler [acc message]
  (or message (inc acc)))

(defn foo-guard [_ message]
  (>= message 10))

(fsm/defsm traffic-light
  [[:green -> :yellow when :to-yellow action `inc-handler]
   [:yellow -> :red when :to-red action `inc-handler]
   [:red -> :green when :to-green guard `foo-guard]])

(def traffic-light-fsm (traffic-light 0))

(deftest fsm-test
  (testing "By default first transition state is the start state"
    (is (-> (traffic-light) :state (= :green))))

  (testing "We can set an accumulator at fsm start"
    (let [fsm (traffic-light 10)]
      (is (-> fsm :value (= 10)))))

  (testing "We can set initial state at fsm start"
    (let [fsm (traffic-light 0 :red)]
      (is (-> fsm :state (= :red)))))

  (testing "An invalid initial state should be ignored"
    (let [fsm (traffic-light 0 :something)]
      (is (-> fsm :state (= :green)))))

  (testing "A valid transition should change the state"
    (is (-> traffic-light-fsm (fsm/send-event :to-yellow) :state (= :yellow))))

  (testing "An invalid transition should not change the state"
    (is (-> traffic-light-fsm (fsm/send-event :to-red) :state (= :green))))

  (testing "The transition action should be executed"
    (is (-> traffic-light-fsm
            (fsm/send-event :to-yellow)
            (fsm/send-event :to-red)
            :value
            (= 2))))

  (testing "We can pass message to transition in send-event"
    (is (-> traffic-light-fsm
            (fsm/send-event :to-yellow)
            (fsm/send-event :to-red -1)
            :value
            (= -1))))

  (testing "Guard is executed before action and prevent the state to change"
    (is (-> traffic-light-fsm
            (fsm/send-event :to-yellow)
            (fsm/send-event :to-red)
            (fsm/send-event :to-green 8)
            :state
            (= :red)))))

