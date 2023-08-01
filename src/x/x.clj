(ns x.x)

(defmacro defsystem
  "Defines a multimethod with first argument 'component', which is
  a [k v].
  Dispatches on k and defines a default defmethod which returns v."
  [sys-name params]
  (when (zero? (count params))
    (throw (IllegalArgumentException. "First argument needs to be component.")))
  `(do

    (defmulti ~(vary-meta sys-name assoc :params (list 'quote params))
      (fn ~(symbol (str (name sys-name))) [& args#]
        ((first args#) 0)))

    (defmethod ~sys-name :default ~params
      (~(first params) 1))

    (var ~sys-name)))

(defmacro defcomponent [k v & sys-impls]
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
                   (str "<" k  "><" sys "> "sys-var " requires " (count sys-params) " args: " sys-params "."
                        " Given " (count fn-params)  " args: " fn-params))))
         `(defmethod ~sys ~k ~fn-params
            (let [~v (~(first fn-params) 1)]
              ~@(rest fn-body)))))
    ~k))

(defn update-map [f m]
  (persistent!
   (reduce-kv (fn [new-map k v]
                (assoc! new-map k (f [k v])))
              (transient {})
              m)))

(defn apply-sys [sys m & args]
  (update-map (fn [c] (apply sys c args)) m))

(defn apply-sys! [sys e & args]
  (doseq [k (keys @e)
          :let [m @e
                c [k (k m)]]]
    (apply sys c e args)))

(defn apply-systems! [[sys-c sys-ce] e & args]
  (let [m (apply apply-sys sys-c @e args)]
    (reset! e m)
    (apply apply-sys! sys-ce e args)
    e))
