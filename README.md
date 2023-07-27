# x.x

A Clojure Entity Component System working on plain atoms, maps and keywords.

# Installation

[![](https://jitpack.io/v/damn/x.x.svg)](https://jitpack.io/#damn/x.x)
```
[com.github.damn/x.x "x"]
```

## Example

``` clojure
(require '[x.x :as x])

(x/defx create)
; => [#'user/create [#'user/create-c #'user/create-e #'user/create!]]

(x/extend-c :a
  (create-c [v] (inc v))
  (create-e [v e] (update e :foo dec))
  (create! [r]
    (println "CREATE A !")
    (swap! r assoc-in [:fooz :bar :baz] 3)))

(x/extend-c :b
  (create-e [v e] (assoc e :babaz :cool))
  (create! [r] (println "B says hi")))

(x/!x! create (atom {:a 0 :b 10 :foo 10}))
; =>
; CREATE A !
; B says hi
; {:a 1, :b 10, :foo 9, :babaz :cool, :fooz {:bar {:baz 3}}}

(x/defx tick)
; => [#'user/tick [#'user/tick-c #'user/tick-e #'user/tick!]]

(x/extend-c :a
  (tick-c [v delta]
    (update v :counter + delta)))

(x/!x! tick (atom {:a {:counter 0}}) 10)
; => {:a {:counter 10}}
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
