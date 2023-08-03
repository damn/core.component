(ns x.x)

(defmacro defsystem
  "A system takes minimum one argument, a component.
  Components are [k v] vectors.
  A system dispatches on k and the default return values is v."
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

(defmacro defcomponent
  "Implements system defmethods for k.

  v is bound over each function and can be used for common destructuring operations."
  [k v & sys-impls]
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

(defn update-map
  "Updates every map-entry with (f [k v])."
  [f m]
  (persistent!
   (reduce-kv (fn [new-map k v]
                (assoc! new-map k (f [k v])))
              (transient {})
              m)))

(defn doseq-entity
  "Calls (f [k (k @e)] e) on each key of e."
  [f e]
  (doseq [k (keys @e)]
    (f [k (k @e)] e))
  e)
