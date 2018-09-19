# fsm-clj documentation
 
 
### coin-operated turnstile

To demonstrate how to use the library the following examples will implement a state machine for a coin-operated [turnstile](https://en.wikipedia.org/wiki/Finite-state_machine#Example:_coin-operated_turnstile).

#### Defining the state machine

```clj
(defsm turnstile
  [[:locked -> :unlocked when :coin]
   [:unlocked -> :locked when :push]])
```

#### Sending an event to the machine

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

#### Passing values on events

When sending an event you can pass a message that will be injected into the action handler.

For example, let's say that now when a coin event is triggered we should pass the total amount with it.

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





