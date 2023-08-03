(ns x.x)

(defmacro defsystem
  "A system is a multimethod which dispatches on a component as first argument.

  Components are [k v] vectors.

  The dispatch function is k and the default return values is v."
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

(defn map-components
  "Updates every component with (apply sys c args)."
  [sys m & args]
  (update-map (fn [c] (apply sys c args)) m))

(defn doseq-components
  "Calls (apply sys c e args) on every map-entry in @e,
  with side effects possible."
  [sys e & args]
  (doseq [k (keys @e)
          :let [m @e
                c [k (k m)]]]
    (apply sys c e args)))

(defn map->doseq-components
  "Calls first map-components and then doseq-components on e."
  [[sys-m sys-d] e & args]
  (let [m (apply map-components sys-m @e args)]
    (reset! e m)
    (apply doseq-components sys-d e args)
    e))
