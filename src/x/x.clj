(ns x.x)

(def ^:private warn-on-override true)

(defmacro defsystem
  "A system is a multimethod which dispatches on ffirst.
  Can be used with defcomponent to check params count at compile time.
  Also gives warnings when overwritten."
  [sys-name params]
  (when (zero? (count params))
    (throw (IllegalArgumentException. "First argument needs to be component.")))
  (when warn-on-override
    (when-let [avar (resolve sys-name)]
      (println "WARNING: Overwriting defsystem:" avar)))
  `(do
    (defmulti ~(vary-meta sys-name assoc :params (list 'quote params))
      (fn ~(symbol (str (name sys-name))) [& args#] (ffirst args#)))
    (var ~sys-name)))

(defmacro defcomponent
  "Implements system defmethods for k.
  v is bound over each function and can be used for common destructuring operations.
  Gives error when the params count does not equal the system params count and gives warnings when
  overwriting a defmethod."
  [k v & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)
                  sys-params (:params (meta sys-var))
                  fn-params (first fn-body)]]
        (do
         (when-not sys-var
           (throw (IllegalArgumentException. (str sys " does not exist."))))
         (when-not (= (count sys-params) (count fn-params)) ; defmethods do not check this, that's why we check it here.
           (throw (IllegalArgumentException.
                   (str sys-var " requires " (count sys-params) " args: " sys-params "."
                        " Given " (count fn-params)  " args: " fn-params))))
         (when (and warn-on-override
                    (get (methods @sys-var) k))
           (println "WARNING: Overwriting defcomponent" k "on" sys-var))
         (when (some #(= % (first fn-params)) (rest fn-params))
           (throw (IllegalArgumentException. (str "First component parameter is shadowed by another parameter at " sys-var))))
         `(defmethod ~sys ~k ~fn-params
            (let [~v (~(first fn-params) 1)]
              ~@(rest fn-body)))))
    ~k))

(defn update-map
  "Recursively calls (assoc m k (apply multimethod [k v] args)) for every k of (keys (methods multimethod)),
  which is non-nil/false in m."
  [m multimethod & args]
  (loop [ks (keys (methods multimethod))
         m m]
    (if (seq ks)
      (recur (rest ks)
             (let [k (first ks)]
               (if-let [v (k m)]
                 (assoc m k (apply multimethod [k v] args))
                 m)))
      m)))

(comment
 (defmulti foo ffirst)

 (defmethod foo :bar [[_ v]] (+ v 2))

 (update-map {} foo)
 {}

 (update-map {:baz 2} foo)
 {:baz 2}

 (update-map {:baz 2 :bar 0} foo)
 {:baz 2, :bar 2}

 )
