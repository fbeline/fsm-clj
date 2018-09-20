# fsm-clj doc

State machines guarantee the behaviour to be always consistent as the rules are written before the machine is started.
With a predefined finite number of states, your application moves from one state to the next based on the
inputs that it receives.
 
The idea is to define high-level transitions and then rely on the state machine to manage state. The machine will be
evaluated at compile time, throwing an exception in case of any parsing issue.

To demonstrate the library usage the following examples will implement a state machine for a coin-operated turnstile.
[more info](https://en.wikipedia.org/wiki/Finite-state_machine#Example:_coin-operated_turnstile).

#### State machine definition

Require the fsm-clj core name space.

```clj
(:require [fsm-clj.core :refer :all])
```

Defining the state machine with `defsm`.

```clj
(defsm turnstile
  [[:locked -> :unlocked when :coin]
   [:unlocked -> :locked when :push]])
```

#### Events

An event is the input responsible for transitions between states.

```clj
(-> (turnstile)
    (send-event :coin)
    :state) ;; => :unlocked
```

#### Actions and Accumulators

Let suppose that every time that a coin is inserted and the turnstile state is changed to open
we want to increment a counter.

```clj
;; action handler
(defn inc-amount [acc _] (inc acc))

(defsm turnstile
  [[:locked -> :unlocked when :coin action `inc-amount]
   [:unlocked -> :locked when :push]])
   
(-> (turnstile 0) ;; start the fsm with accumulator value equals 0
    (send-event :coin)
    (send-event :push)
    (send-event :coin)
    :value) ;; => 2
```

The action handler returns the new accumulator value that will be passed forward.

#### Events with payload

When sending an event you can pass a message that will be injected into the action handler.

For example, let's say that when a coin transition is triggered we should pass the coin value with it:

```clj
;; action handler
(defn inc-amount [acc amount] (+ acc amount))

(defsm turnstile
  [[:locked -> :unlocked when :coin action `inc-amount]
   [:unlocked -> :locked when :push]])
   
(-> (turnstile 0) ;; start the fsm with accumulator value equals 0
    (send-event :coin 50)
    (send-event :push)
    (send-event :coin 100)
    :value) ;; => 150
```

#### Guard

Guard is a function responsible to determine if a state should change or not.

Supposing that the minimum coin value to unlock the turnstile should be 50.

```clj
;; action handler
(defn inc-amount [acc amount] (+ acc amount))

(defn guard-handler [_state amount]
  (when (>= amount 50)))

(defsm turnstile
  [[:locked -> :unlocked when :coin action `inc-amount guard `guard-handler]
   [:unlocked -> :locked when :push]])
   
(-> (turnstile 0) ;; start the fsm with accumulator value equals 0
    (send-event :coin 25)
    :state) ;; => :lock
```

The guard handler must be a function with two parameters, being the first the current state and the second the message
sent by `send-event`.
