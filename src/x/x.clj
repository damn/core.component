(ns x.x)

; TODO  ?
; [clojure.tools.macro :refer (name-with-attributes)]

(defmacro defsystem
  {:arglists '([name [params*] default-return-value?])}
  [sys-name params & default-return-value]
  (when (zero? (count params))
    (throw (IllegalArgumentException. "Requires at least 1 param for dispatching.")))
  `(do
    (defmulti ~(vary-meta sys-name assoc :params (list 'quote params))
      (fn ~(symbol (str (name sys-name))) [~@params] ~(first params)))
    (defmethod ~sys-name :default ~params ~@default-return-value)
    (var ~sys-name)))

(defmacro defcomponent [c & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)
                  sys-params (:params (meta sys-var))
                  fn-params (first fn-body)]]
        (do
         (when-not sys-var
           (throw (IllegalArgumentException. (str sys " does not exist."))))
         (when-not (= (count sys-params) (count fn-params))
           (throw (IllegalArgumentException.
                   (str "<" c  "><" sys "> "sys-var " requires " (count sys-params) " args: " sys-params "."
                                   " Given " (count fn-params)  " args: " fn-params))))
         `(defmethod ~sys ~c ~@fn-body)))
    ~c))

(defn update-map [f m]
  (persistent!
   (reduce-kv (fn [new-map k v]
                (assoc! new-map k (f k v)))
              (transient {})
              m)))

; TODO I could check (keys (methods sys)) and only update as needed
; transient works with 'clojure.core/update' ? https://groups.google.com/g/clojure/c/h5ks9KDHQmE
; => perf tests

(defn apply-sys [sys e & args]
  (update-map (fn [c v] (apply sys c v args))
              e))

(defn apply-sys! [sys r & args]
  (doseq [c (keys @r)]
    (apply sys c r args)))

(defmacro defsystems [sys-name [vsys rsys] & {:keys [extra-params]}]
  `(let [systems# [(defsystem ~vsys [~'c ~'v ~@extra-params] ~'v)
                   (defsystem ~rsys [~'c ~'r ~@extra-params])]]
     [(def ~sys-name systems#) systems#]))

(defn apply-systems! [[sys-v sys-r] r & args]
  (let [e (apply apply-sys sys-v @r args)]
    (reset! r e)
    (apply apply-sys! sys-r r args)
    r))
