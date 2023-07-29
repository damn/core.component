# <p align="center"> x.x - gamedev language </p>

A Clojure [Entity Component System](https://en.wikipedia.org/wiki/Entity_component_system) DSL which is working with plain atoms, maps, keywords and multimethods. 
No type classes involved. No dependencies and only 60 LOC.

This system is not a tech demo but has been used since a few months in an action RPG game I am developing. (Unreleased yet).
You can see an application in [gdl, a clojure game engine](https://github.com/damn/gdl).

# Why gamedev language?

I want to create language extensions (which is simple in clojure through macros) and [libraries](https://github.com/damn/gdl) with the goal to make it more simple, easy and fn to design emergent systems.

# Rationale

Because game state is stored in different atoms for each entity, we have to make use with side effects.
But for functions which only handle local component state we want to make them purely functional.
x.x allows us to separate both functions with side effects and pure functions cleanly.

# Installation

[![](https://jitpack.io/v/damn/x.x.svg)](https://jitpack.io/#damn/x.x)
```
[com.github.damn/x.x "x.1"]
```

## Glossary

Abbreviation | Meaning | Datatype
----- | ----    | ----
 c   | component type  | keyword
 v   | component value | anything
 e   | entity            | map
 r   | entity-reference  | atom
 sys | system            | multimethod

Note that the main system works just with plain maps
and using atoms and systems with side-effects do not need to be used.

## Example

There are only 2 macros: `defsystem` and `extend-component`. Systems can be applied with `apply-sys` or `apply-sys!` for systems without and with side-effects.

There is also a convenience macro `defsystems` for defining one pure and one system with side effects and `apply-systems!` for applying them both to an atom reference of an entity.

Because systems are just plain multimethods which dispatch on the first argument, you can also easily compose and extend systems with any other functions.

``` clojure

; How to create an entity:
(def e {:foo :fooz :bar :baz}
; Now we want to add a component:
(def e (assoc e :mouseover? true))
; remove a component:
(def e (dissoc e :mouseover?)
; entities are just maps and components just keywords&values, so it is totally simple to use!

(require '[x.x :as x])

; the tick system updates entities in game logic and passes delta time in elapsed ms since last update
(x/defsystem tick [c v delta] v)
; v is defined as default return value for components which do not implement the system
; which means they are not updated on apply-sys.

(x/extend-component :a
  (tick [_ v delta]
    (update v :counter + delta)))

(x/apply-sys tick {:a {:counter 0}} 10)
; {:a {:counter 10}}

; because systems are normal functions/multimethods you can just call them directly also
; on specific components
(tick :a {:counter 0} 3)
; {:counter 3}

; all defsystems need to have a first argument 'c' for the component-type. (a clojure keyword).
(x/defsystem create  [c v] v) ; a pure system which updates value, like tick. But with no extra argument.
(x/defsystem create! [c r]) ; for side-effects

(x/extend-component :a
  (create [_ v] (inc v)) ; the first argument is not used, it is a reference to the keyword :a
  (create! [_ r] 
    (println "CREATE A !")
    (swap! r assoc-in [:fooz :bar :baz] 3)))

(x/extend-component :b
  (create! [_ r]
    (println "B says hi")))

(def create-systems [create create!])

(x/apply-systems! create-systems (atom {:a 0 :b 10 :foo 10}))
; CREATE A !
; B says hi
; #object[clojure.lang.Atom 0x7daf5b58 {:status :ready, :val {:a 1, :b 10, :foo 10, :fooz {:bar {:baz 3}}}}]
```

# See the system being used

[Here x.x is used for managing creation/destruction and ticks of entities.](https://github.com/damn/gdl/blob/a864c000cffb41843dce3b5575461ba098f7f921/src/gdl/ecs.clj)

# Other Clojure ECS

Difference to x.x: I have not tried those yet, but as far as I have researched they are using special types to wrap components/entities and not operating on plain data.

* https://github.com/markmandel/brute
* https://github.com/muhuk/clecs
* https://github.com/weavejester/ittyon
* https://github.com/joinr/spork/blob/master/src/spork/entitysystem/store.clj

# Future work

* Try with datomic: not walk all entity keys all the time but query directly for components. Time-travel, serialization of gamestate, history of transactions for networking etc.


## License

Copyright © 2023 Michael Sappler

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

