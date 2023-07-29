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
         (when-not sys-var
           (throw (IllegalArgumentException. (str sys " does not exist."))))
         (when-not (= (count sys-params) (count fn-params))
           (throw (IllegalArgumentException.
                   (str "<" c  "><" sys "> "sys-var " requires " (count sys-params) " args: " sys-params "."
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

(defn doseq-e!
  "Doseq's over (keys e), calling a system with params [c e & more]"
  [sys e & args]
  (doseq [c (keys e)]
    (apply sys c e args)))

(defn !x! [[sys-v sys-e sys-r] r & args] ; TODO just x! or apply!
  (let [e (apply reduce-v sys-v @r args)
        e (apply reduce-e sys-e  e args)]
    (reset! r e)
    (apply doseq-r! sys-r r args)
    r))

; TODO does not handle extra args yet ! ~@extra-args in each defsystem.

(defmacro defsystems [sys-name [vsys esys rsys] & {:keys [extra-params]}]
  `(let [systems# [(defsystem ~vsys [~'c ~'v ~@extra-params] ~'v)
                   (defsystem ~esys [~'c ~'e ~@extra-params] ~'e)
                   (defsystem ~rsys [~'c ~'r ~@extra-params])]]
     [(def ~sys-name systems#) systems#]))

(defn intern-clojure []
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
