# <p align="center"> x.x </p>
<p align="center">
<image src="https://kiss.one/lotus_centered.jpg" width="250" height="200"/>
</p>

A Clojure [Entity Component System](https://en.wikipedia.org/wiki/Entity_component_system) DSL which works with plain atoms, maps, keywords and multimethods.
No type classes involved. No dependencies and only 60 LOC.

# Useful links

[Why game programming is broken](https://namekdev.net/2017/05/exploring-directions-to-fix-game-programming/)

[Overwatch ECS architecture GDC video](https://youtu.be/zrIY0eIyqmI)

# Rationale

Because game state is stored in different atoms for each entity, we have to make use of side effects.
But for functions which only handle local component state we want to make them purely functional.
x.x allows us to separate functions with side effects and pure functions cleanly.

# Installation

[![](https://jitpack.io/v/damn/x.x.svg)](https://jitpack.io/#damn/x.x)
``` clojure
[com.github.damn/x.x "main-SNAPSHOT"]
```

## Glossary

Abbreviation | Meaning           | Datatype
-----        | ----              | ----
 k           | key               | keyword
 v           | value             | anything
 c           | component         | [k v]
 m           | entity value      | map of components
 e           | entity            | atom
 sys         | system            | multimethod

Note that the main system works just with plain maps. Atoms and systems with side-effects do not need to be used.

## Example

There are only 2 macros: `defsystem` and `defcomponent`. Systems can be applied with `apply-sys` or `apply-sys!` for systems without and with side-effects.

There is also a convenience macro `defsystems` for defining one pure and one system with side effects and `apply-systems!` for applying them both to an atom reference of an entity.

Because systems are just plain multimethods which dispatch on the first argument, you can also easily compose and extend systems with any other functions.

All systems take `c` as first argument, which is just `[k v]` and the default return value is `v`.

``` clojure

; How to create an entity:
(def e {:foo :fooz :bar :baz}
; Now we want to add a component:
(def e (assoc e :mouseover? true))
; remove a component:
(def e (dissoc e :mouseover?)
; entities are just maps and components just keywords&values, so it is totally simple to use!

(require '[x.x :refer :all])

; the tick system updates entities in game logic and passes delta time in elapsed ms since last update
(defsystem tick [c delta])

; value v is bound over each function, but can also be accessed in the first argument [k v]
(defcomponent :a v
  (tick [_ delta]
    (update v :counter + delta)))

(apply-sys tick {:a {:counter 0}} 10)
; {:a {:counter 10}}

; because systems are normal functions/multimethods you can just call them directly also
; on specific components
(tick [:a {:counter 0}] 3)
; {:counter 3}

(defsystem create  [c]) ; a pure system which updates value, like tick. But with no extra argument.
(defsystem create! [c e]) ; for side-effects

(defcomponent :a v
  (create [_] (inc v))
  (create! [_ e]
    (println "CREATE A !")
    (swap! e assoc-in [:fooz :bar :baz] 3)))

(defcomponent :b v
  (create! [_ e]
    (println "B says hi")))

(def create-systems [create create!])

(apply-systems! create-systems (atom {:a 0 :b 10 :foo 10}))
; CREATE A !
; B says hi
; #object[clojure.lang.Atom 0x7daf5b58 {:status :ready, :val {:a 1, :b 10, :foo 10, :fooz {:bar {:baz 3}}}}]
```

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

