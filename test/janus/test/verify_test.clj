(ns janus.test.verify-test
  [:use janus.verify
   janus.dsl
   midje.sweet]
  [:require [clj-http.client :as http]
   [clojure.data.json :as json]])

(defn response-with-header [k v]
  {:headers {k v}})

(tablular
 (fact "a header clause should allow equality and matching checks"
       (check-clause ?response (header ?k ?v) => ?expected)
       (check-clause (response-with-header "ct" "blah")  (header "ct" "blah")) => empty?
       
       (check-clause {:headers {"ct" "foo"}} 
                     [:header {:name "ct" :value "bar"}]) => ["Expected header 'ct' to equal 'bar'. Got 'foo'."]
                     (check-clause {:headers {}} 
                                   [:header {:name "ct" :value "bar"}]) => ["Expected header 'ct' to equal 'bar', but was missing."])
)

(fact "special handling for content type header - should ignore encoding"
      (check-clause {:headers {"content-type" "foo/bar;charset=whatever"}} 
                    [:header {:name "content-type" :value "foo/bar"}]) => empty?)
(fact "reponse with different status or ct fails"
      
      (validate-response [[:status 201]]  {:result :ok :response {:status 200}}  {}) => ["Expected status 201. Got status 200"]
      
      (validate-response  [[:header {:name "ct" :value "text/html"}]] {:result :ok :response {:headers {"ct" "app/json"}}} {})
      => ["Expected header 'ct' to equal 'text/html'. Got 'app/json'."])

(fact "the body is checked depending on body type spec"
      (validate-response  [[:json-body [[:foo :bar]]]] 
                          {:result :ok :response {:body "{}"}}  {})
  => [..json-validation..]
  (provided (janus.json-response/verify-document "{}" [[:foo :bar]])
            => [..json-validation..]
            (check-body-clause anything anything) => []))

(fact "fails if comes across a clause it doesnt understand"
      (validate-response [[:json-body [[:foo 1]]]] 
                         {:result :ok :response  {:body "{}"}} {}) 
      => (contains "CONTRACT ERROR: :foo is not an understood body clause"))

(fact "check-clause for body delegates to the body checking stuff for each clause"
      (check-clause ..actual.. [:json-body [..clause1.. ..clause2..]]) => [..res1.. ..res2..]
      (provided
       ..actual.. =contains=> {:body ..raw-body..}
       (safe-parse :json ..raw-body..) => {:result :success :parsed ..body..}
       (janus.json-response/verify-document anything anything) => []
       (check-body-clause ..body.. ..clause1..) => [..res1..]
       (check-body-clause ..body.. ..clause2..) => [..res2..]))

(fact "checking-body-clause evals fn and prints out contextual error message"
      (check-body-clause {} [:fn :foo "my-fn"]) => ["Expected my-fn to be non-nil on {}"]
      (check-body-clause {:foo 1} [:fn :foo "my-fn"]) => [])

(fact "checking fn based clauses checks subclauses too if sub passes"
      (check-body-clause {:foo 1} [:fn :foo {:key :foo} 
                                   [[:predfn number? {:type :number}]]]) => [])

(fact "all clause validates both constraints"
      (check-body-clause {:foo 1 :bar 1} [:all [[:fn :foo :foo] [:fn :bar :bar]]]) => []
      (check-body-clause {:fo 1 :bar 1} [:all [[:fn :foo :foo] [:fn :bar :bar]]]) => ["Expected :foo to be non-nil on {:bar 1, :fo 1}"]
      (check-body-clause {:foo 1 :ba 1} [:all [[:fn :foo :foo] [:fn :bar :bar]]]) => ["Expected :bar to be non-nil on {:foo 1, :ba 1}"])

(fact "Length constraints apply to strings as well as colls" 
      (check-body-clause "Short" [:all [[:predfn string? {:type :string}] [:with-length-between 1 20]]]) => empty?
      (check-body-clause "FarTooLong" [:all [[:predfn string? {:type :string}] [:with-length-between 1 5]]]) => ["Expected FarTooLong to have length between 1 and 5"])


(fact "Constraining numerics to ranges" 
      (check-body-clause 23 [:in-range {:min 20 :max 50}]) => empty?
      (check-body-clause 1 [:in-range {:min 20 :max 50}]) => ["Expected 1 to be between 20 and 50"])
(fact "Errors when checking range on non numeric"
      (check-body-clause "foo" [:in-range {:min 20 :max 50}]) => ["Attempted to constrain range of foo which is not a number"])

(fact "Each validates that the given is a coll and validates each against it"
      (check-body-clause [1,2,3] [:each [[:predfn number? {:type :number}]
                                                [:predfn #(>= % 1) {:greater-than 1}]]])=> []
      (check-body-clause [1,2,3] [:each [[:predfn string? {:type :string}]
                                         [:predfn #(= "bar" %) {:equals "bar"}]]])=>
                                         (contains ["Expected 2 to be {:equals \"bar\"}"
                                                    "Expected 2 to be {:type :string}"] :in-any-order))
;; todo : if it ait a coll?
(fact "with-length-between checks something has a sufficient count"
      (check-body-clause [1,2,3] [:with-length-between 0 5])=> []
      (check-body-clause [1,2,3] [:with-length-between 6 10])=> ["Expected [1 2 3] to have length between 6 and 10"]
      (check-body-clause [1,2,3] [:with-length-between 0 1])=> ["Expected [1 2 3] to have length between 0 and 1"])

(fact "checking fn based clause bubbles up error messages from sub clauses with context"
      (check-body-clause {:foo "bob"} [:fn :foo {:key :foo} 
                                   [[:predfn number? {:type :number}]]]) => 
                                   ["Expected bob to be {:type :number}"])

(against-background [..contract.. =contains=> {:properties []}
                     ..context.. =contains=> {:properties []}]
  (fact "property values are extracted from the contract"
    (property "prop" ..contract.. ..context..) => "contract val"
    (provided
      ..contract.. =contains=> {:properties [{:name "prop" :value "contract val"}]})
    
    (property "prop" ..contract.. ..context..) => "first"
    (provided
      ..contract.. =contains=> {:properties [{:name "prop" :value "first"}
                                             {:name "prop" :value "second"}]})

    (property "prop" ..contract.. ..context..) => nil)

  (fact "property values are extracted from the context"
    (property "prop" ..contract.. ..context..) => "context val"
    (provided
      ..context.. =contains=> {:properties [{:name "prop" :value "context val"}]})))


(fact
  (to-xml [:tag]) => (contains "<tag></tag>")
  (to-xml [:tag {:attr "value"}]) => (contains "<tag attr=\"value\">")
  (to-xml [:tag [:sub]]) => (contains "<tag><sub></sub>")
  (to-xml [:tag "blah"]) => (contains "<tag>blah</tag>")
  (to-xml [:tag "blah" [:sub {:attr "val"}]]) => (contains "<tag>blah<sub attr=\"val\"></sub></tag"))


(facts
 (verify-service {:name "svc" :contracts [{:name "c1"}]} ..context..) =>  {:service "svc" :results {"c1" ..res..}}
  (provided
   (verify-contract {:name "c1"} ..context..) => ..res..))
