# <p align="center"> x.x </p>

A Clojure [Entity Component System](https://en.wikipedia.org/wiki/Entity_component_system) which works with plain atoms, maps, keywords and multimethods.

# Installation

[![](https://jitpack.io/v/damn/x.x.svg)](https://jitpack.io/#damn/x.x)
``` clojure
[com.github.damn/x.x "main-SNAPSHOT"]
```

# Documentation

* [API docs](https://damn.github.io/x.x/)

## Glossary

Abbreviation | Meaning           | Datatype
-----        | ----              | ----
 k           | key               | keyword
 v           | value             | anything
 c           | component         | [k v]
 m           | entity value      | map of components
 e           | entity            | atom
 sys         | system            | multimethod

## Example

``` clojure
(require '[x.x :refer :all])

; the tick system updates entities in game logic and passes delta time in elapsed ms since last update
(defsystem tick [c delta])

; value v is bound over each function, but can also be accessed in the first argument [k v]
(defcomponent :a v
  (tick [_ delta]
    (update v :counter + delta)))

(map-components tick {:a {:counter 0}} 10)
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

; this is a convenience function to apply one
; pure and one system with side-effects after another
(map->doseq-components [create create!] (atom {:a 0 :b 10 :foo 10}))
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

