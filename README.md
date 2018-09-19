# fsm-clj

A Clojure library designed to create Finite State Machines.

- Easy to use flat one level state machine.
- Define state machines that can accumulate values.
- Transitions with actions.
- Graphically visualize the resulting state machines.

## Usage


## Quick Start

Require the fsm-clj core name space.

```clj
(:require [fsm-clj.core :refer :all])
```

Define a simple traffic light State Machine.

```clj
(defsm traffic-light
  [[:green -> :yellow when :to-yellow]
   [:yellow -> :red when :to-red]
   [:red -> :green when :to-green]])
```

Send an event to it.

```clj
(-> (traffic-light)
    (send-event :to-yellow)
    :state) ;; => :yellow
```

You can graphically generate the State Machine (open a Swing viewer).

```clj
(show! (traffic-light))
```

![Traffic Light Finite State Machine](doc/fsm-traffic-light.png)

## Documentation
Refer to the [documentation](doc/documentation.md) for more detailed information as:

- Transitions with actions
- Transitions guard
- State Machines with accumulator

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
