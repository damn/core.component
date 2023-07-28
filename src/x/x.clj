(ns x.x)

(defmacro defsystem
  {:arglists '([name [params*] default-return-value?])}
  [sys-name params & default-return-value]
  (when (zero? (count params))
    (throw (IllegalArgumentException. "Requires at least 1 param for dispatching.")))
  `(do
    (defmulti ~(vary-meta sys-name assoc :params (list 'quote params))
      (fn [~@params] ~(first params)))
    (defmethod ~sys-name :default ~params ~@default-return-value)
    (var ~sys-name)))

(defmacro extend-component [c & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)
                  sys-params (:params (meta sys-var))
                  fn-params (first fn-body)]]
        (do
         (when-not (= (count sys-params) (count fn-params))
           (throw (IllegalArgumentException.
                   (str sys-var " requires " (count sys-params) " args: " sys-params "."
                                   " Given " (count fn-params)  " args: " fn-params))))
         `(defmethod ~sys ~c ~@fn-body)))
    ~c))

(defn reduce-v
  "Applies a system with params [c v & more] on an entity e, updating v for every c."
  [sys e & args]
  (reduce (fn [e c]
            (update e c #(apply sys c % args)))
          e
          (keys e)))

(defn reduce-e
  "Applies a system with params [c e & more] on an entity e, updating e for every c."
  [sys e & args]
  (reduce (fn [e c]
            (apply sys c e args))
          e
          (keys e)))

(defn doseq-r!
  "Doseq's over (keys @r), calling a system with params [c r & more]"
  [sys r & args]
  (doseq [c (keys @r)]
    (apply sys c r args)))

(defn !x! [[sys-v sys-e sys-r] r & args]
  (let [e (apply reduce-v sys-v @r args)
        e (apply reduce-e sys-e  e args)]
    (reset! r e)
    (apply doseq-r! sys-r r args)
    r))

; this hurts the rule of clarity & explicity -> too much magic?
; also hidden functions which grep cannot find...
; rather declare 3 systems explicitly, there are not so many systems in a game usually ?
(comment
 (defmacro defsystem-v [name]
   `(defsystem ~name [~'c ~'v] ~'v))
 ; defsystem-e, defsystem-r, ...

 (macroexpand-1 '(defsystem-v create))

 (defmacro defsystems [sys-name]
   (let [suffix #(symbol (str (name sys-name) %))
         ssys (suffix "-systems")
         vsys sys-name
         esys (suffix "-e")
         rsys (suffix "-!")]
     `(let [systems# [(defsystem ~vsys [~'c ~'v] ~'v)
                      (defsystem ~esys [~'c ~'e] ~'e)
                      (defsystem ~rsys [~'c ~'r])]]
        [(def ~ssys systems#) systems#]))))
