# <p align="center"> x.x - gamedev language </p>

A Clojure [Entity Component System](https://en.wikipedia.org/wiki/Entity_component_system) working on plain atoms, maps and keywords.

> Entity component system (ECS) is a software architectural pattern mostly used in video game development for the representation of game world objects. An ECS comprises entities composed from components of data, with systems which operate on entities' components.
>
> ECS follows the principle of composition over inheritance, meaning that every entity is defined not by a type hierarchy, but by the components that are associated with it. Systems act globally over all entities which have the required components.

# Rationale

Because game state is stored in different atoms for each entity, we have to make use with side effects.
But for functions which only handle local component/entity state we want to make them purely functional.
x.x allows us to separate both functions with side effects and pure functions cleanly.

# Future work

* Transients in the reduce fns

* Datomic? (not walk all entity keys all the time but query directly for components)

# Installation

[![](https://jitpack.io/v/damn/x.x.svg)](https://jitpack.io/#damn/x.x)
```
[com.github.damn/x.x "x.1"]
```

## Glossary

Abbreviation | Meaning | Datatype
----- | ----    | ----
 c   | (component) type  | keyword
 v   | (component) value | anything
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

; because systems are normal multimethods you can just call them directly also
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

