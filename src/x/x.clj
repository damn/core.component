(ns x.x)

(defmacro defcs [sym] `(defmulti ~sym (fn [c# & _#] c#)))

(defn def-def [cs suffix]
  (case suffix
    "-c" (defmethod cs :default [c v   & _] v)
    "-e" (defmethod cs :default [c v e & _] e)
    "!"  (defmethod cs :default [c r   & _]  )))

(defmacro defx [sym]
  (let [cs `(vector
             ~@(for [s ["-c" "-e" "!"]
                     :let [cs (symbol (str (name sym) s))]]
                 `(do (defcs ~cs) (def-def ~cs ~s) (var ~cs))))]
    `(let [cs# ~cs]
       [(def ~sym cs#) cs#])))

(defn reduce-apply-c [cs e & args] (reduce (fn [e c] (update e c #(apply cs c % args))) e (keys e)))
(defn reduce-apply-e [cs e & args] (reduce (fn [e c] (apply cs c (c e) e args))         e (keys e)))
(defn doseq-apply-r! [cs r & args] (doseq [[c v] @r] (apply cs c r args))) ; do not pass v, might change

(defn !x! [[cs-c cs-e cs-r] r & args]
   (let [e (apply reduce-apply-c cs-c @r args)
         e (apply reduce-apply-e cs-e  e args)]
     (reset! r e)
     (apply doseq-apply-r! cs-r r args)
     @r))

(defn extend-cs [cs c f] (defmethod cs c [_ & args] (apply f args)))

(defmacro extend-c [c & cs-impls]
  `(do
    ~@(for [[cs & fn-b] cs-impls
            :let [fn-n (symbol (str (name c) "-" (name cs)))
                  fn-f `(fn ~fn-n ~@fn-b)]]
        `(extend-cs ~cs ~c ~fn-f))
    ~c))
