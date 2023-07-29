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
         (when-not sys-var
           (throw (IllegalArgumentException. (str sys " does not exist."))))
         (when-not (= (count sys-params) (count fn-params))
           (throw (IllegalArgumentException.
                   (str "<" c  "><" sys "> "sys-var " requires " (count sys-params) " args: " sys-params "."
                                   " Given " (count fn-params)  " args: " fn-params))))
         `(defmethod ~sys ~c ~@fn-body)))
    ~c))

(defn reduce-vs [f m]
  (persistent!
   (reduce-kv (fn [new-map k v]
                (assoc! new-map k (f k v)))
              (transient {})
              m)))

(defn reduce-v [sys e & args] (reduce-vs (fn [c v]  (apply sys c v args)) e))
(defn doseq-e! [sys e & args] (doseq [c (keys e)]   (apply sys c e args)))
(defn doseq-r! [sys r & args] (doseq [c (keys @r)]  (apply sys c r args)))

(defn !x! [[sys-v sys-r] r & args] ; TODO just x! or apply!
  (let [e (apply reduce-v sys-v @r args)]
    (reset! r e)
    (apply doseq-r! sys-r r args)
    r))

(defmacro defsystems [sys-name [vsys rsys] & {:keys [extra-params]}]
  `(let [systems# [(defsystem ~vsys [~'c ~'v ~@extra-params] ~'v)
                   (defsystem ~rsys [~'c ~'r ~@extra-params])]]
     [(def ~sys-name systems#) systems#]))

#_(defn intern-clojure []
  (in-ns 'clojure.core)
  (require 'potemkin.namespaces)

  (let [nmspace "x.x"
        publics (butlast (keys (ns-publics (symbol nmspace))))]

    (println "publics: " (count publics))
    (println publics)

    (doseq [sym publics]
      (println sym)

      ; TODO warns/throws on refresh-all
      (when (ns-resolve 'clojure.core sym)
        (throw (IllegalArgumentException. (str "sym cannot be interned: " (name sym))))))

    (let [syms (map #(symbol nmspace (name %)) publics)]
      (eval
       `(potemkin.namespaces/import-vars ~@syms)))

    (in-ns (symbol nmspace))))
