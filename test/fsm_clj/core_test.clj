(ns fsm-clj.core-test
  (:require [clojure.test :refer :all]
            [fsm-clj.core :refer :all]))

(defsm simple-turnstile
  [[:locked -> :unlocked when :coin]
   [:unlocked -> :locked when :push]])
(deftest simple-transitions
  (testing "Basic state transition"
    (is (-> (simple-turnstile)
            (send-event :coin)
            :state
            (= :unlocked)))))

(defn inc-amount [acc _] (inc acc))
(defsm turnstile-with-accumulator
  [[:locked -> :unlocked when :coin action `inc-amount]
   [:unlocked -> :locked when :push]])
(deftest accumulators+actions
  (testing "The accumulator is properly calculated"
    (is (-> (turnstile-with-accumulator 0)
            (send-event :coin)
            (send-event :push)
            (send-event :coin)
            :value
            (= 2)))))

(defn add-amount [acc amount] (+ acc amount))
(defsm turnstile-with-accumulator2
  [[:locked -> :unlocked when :coin action `add-amount]
   [:unlocked -> :locked when :push]])
(deftest events-with-payload
  (testing ""
    (is (-> (turnstile-with-accumulator2 0)
            (send-event :coin 50)
            (send-event :push)
            (send-event :coin 100)
            :value
            (= 150)))))

(defn guard-handler [_state amount]
  (>= amount 50))
(defsm turnstile-with-guard
  [[:locked -> :unlocked when :coin action `add-amount guard `guard-handler]
   [:unlocked -> :locked when :push]])

(deftest guard
  (testing "An amount < than 50 should not unlock the turnstile"
    (is (-> (turnstile-with-guard 0)
            (send-event :coin 25)
            :state
            (= :locked))))
  (testing "An Amount greater or equal 50 should unlock the turnstile"
    (is (-> (turnstile-with-guard 0)
            (send-event :coin 50)
            :state
            (= :unlocked)))))


;; generic traffic light fsm tests


(defn inc-handler [acc message]
  (or message (inc acc)))
(defn foo-guard [_ message]
  (>= message 10))
(defsm traffic-light
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
    (is (-> traffic-light-fsm (send-event :to-yellow) :state (= :yellow))))

  (testing "An invalid transition should not change the state"
    (is (-> traffic-light-fsm (send-event :to-red) :state (= :green))))

  (testing "The transition action should be executed"
    (is (-> traffic-light-fsm
            (send-event :to-yellow)
            (send-event :to-red)
            :value
            (= 2))))

  (testing "We can pass message to transition in send-event"
    (is (-> traffic-light-fsm
            (send-event :to-yellow)
            (send-event :to-red -1)
            :value
            (= -1))))

  (testing "Guard is executed before action and prevent the state to change"
    (is (-> traffic-light-fsm
            (send-event :to-yellow)
            (send-event :to-red)
            (send-event :to-green 8)
            :state
            (= :red)))))

