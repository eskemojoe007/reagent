(ns demo
  (:require [cloact.core :as cloact :refer [atom]]
            [clojure.string :as string]
            [demoutil :as demoutil :refer-macros [get-source]]
            [cloact.debug :refer-macros [dbg println]]))

(defn src-parts [src]
  (string/split src #"\n(?=[(])"))

(defn src-defs [parts]
  (let [ws #"\s+"]
    (into {} (for [x parts]
               [(-> x (string/split ws) second keyword) x]))))

(def srcmap
  (-> "demo.cljs" get-source src-parts src-defs))

(def nssrc
  "(ns example
  (:require [cloact.core :as cloact :refer [atom]]))
")

(defn src-for-names [names]
  (string/join "\n" (-> srcmap
                        (assoc :ns nssrc)
                        (select-keys names)
                        vals)))

(defn src-for [defs]
  [:pre (-> defs src-for-names demoutil/syntaxify)])

(defn demo-component [{:keys [comp defs src]}]
  (let [colored (if src
                  (demoutil/syntaxify src)
                  (src-for defs))
        showing (atom true)]
    (fn []
      [:div
       (when comp
         [:div.demo-example
          [:a.demo-example-hide {:on-click (fn [e]
                                             (.preventDefault e)
                                             (swap! showing not))}
           (if @showing "hide" "show")]
          [:h3.demo-heading "Example "]
          (when @showing
            (if defs
              [:div.simple-demo [comp]]
              [comp]))])
       (when @showing
         [:div.demo-source
          [:h3.demo-heading "Source"]
          colored])])))

(defn simple-component []
  [:div
   [:p "I am a component!"]
   [:p.someclass 
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."]])

(defn simple-parent []
  [:div
   [:p "I include simple-component."]
   [simple-component]])

(defn lister [props]
  [:ul
   (for [item (:items props)]
     [:li {:key item} "Item " item])])

(defn lister-user []
  [:div
   "Here is a list:"
   [lister {:items (range 3)}]])

(def click-count (atom 0))

(defn counting-component []
  [:div
   "The atom " [:code "click-count"] " has value: "
   @click-count ". "
   [:input {:type "button" :value "Click me!"
            :on-click #(swap! click-count inc)}]])

(defn atom-input [{:keys [value]}]
  [:input {:type "text"
           :value @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn shared-state []
  (let [val (atom "foo")]
    (fn []
      [:div
       [:p "The value is now: " @val]
       [:p "Change it here: "
        [atom-input {:value val}]]])))

(defn timer-component []
  (let [seconds-elapsed (atom 0)]
    (fn []
      (js/setTimeout #(swap! seconds-elapsed inc) 1000)
      [:div
       "Seconds Elapsed: " @seconds-elapsed])))

(defn render-simple []
  (cloact/render-component [simple-component]
                           (.-body js/document)))

(defn calc-bmi [params to-calc]
  (let [{:keys [height weight bmi]} params
        h (/ height 100)]
    (case to-calc
      :bmi (assoc params :bmi (/ weight (* h h)))
      :weight (assoc params :weight (* bmi h h)))))

(def bmi-data (atom (calc-bmi {:height 180 :weight 80} :bmi)))

(defn set-bmi [key val]
  (swap! bmi-data #(calc-bmi (assoc % key val)
                             (case key :bmi :weight :bmi))))

(defn slider [{:keys [value min max param]}]
  [:input {:type "range" :value value :min min :max max
           :style {:width "100%"}
           :on-change #(set-bmi param (-> % .-target .-value))}])

(defn bmi-component []
  (let [{:keys [weight height bmi]} @bmi-data
        [color diagnose] (cond
                          (< bmi 18.5) ["orange" "underweight"]
                          (< bmi 25) ["inherit" "normal"]
                          (< bmi 30) ["orange" "overweight"]
                          :else ["red" "obese"])]
    [:div
     [:h3 "BMI calculator"]
     [:div
      "Height: " (int height) "cm"
      [slider {:value height :min 100 :max 220 :param :height}]]
     [:div
      "Weight: " (int weight) "kg"
      [slider {:value weight :min 30 :max 150 :param :weight}]]
     [:div
      "BMI: " (int bmi) " "
      [:span {:style {:color color}} diagnose]
      [slider {:value bmi :min 10 :max 50 :param :bmi}]]]))

(defn intro []
  [:div.demo-text
   
   [:h2 "Introduction to Cloact"]

   [:p [:a {:href "https://github.com/holmsand/cloact"} "Cloact"]
    " provides a minimalistic interface between "
    [:a {:href "https://github.com/clojure/clojurescript"} "ClojureScript"]
    " and " [:a {:href "http://facebook.github.io/react/"} "React"]
    ". It allows you to define efficient React components using nothing but
    plain ClojureScript functions, that describe your UI using a " [:a
    {:href "https://github.com/weavejester/hiccup"} "Hiccup"] "-like
    syntax."]

   [:p "A very basic component may look something like this: "]
   [demo-component {:comp simple-component
                    :defs [:simple-component]}]

   [:p "You can build new components using other components as
   building blocks. Like this:"]
   [demo-component {:comp simple-parent
                    :defs [:simple-parent]}]

   [:p "Data is passed to child components using plain old Clojure
   maps. For example, here is a component that shows items in a "
    [:code "seq"] ":" ]

   [demo-component {:comp lister-user
                    :defs [:lister :lister-user]}]

   [:p [:strong "Note: "]
    "The " [:code "{:key item}"] " part of the " [:code ":li"] " isn't
    really necessary in this simple example, but passing a unique key
    for every item in a dynamically generated list of components is
    good practice, and helps React to improve performance for large
    lists."]])

(defn managing-state []
  [:div.demo-text
   [:h2 "Managing state in Cloact"]

   [:p "The easiest way to manage state in Cloact is to use Cloact's
   own version of " [:code "atom"] ". It works exactly like the one in
   clojure.core, except that it keeps track of every time it is
   deref'ed. Any component that uses an " [:code "atom"]" is automagically
   re-rendered when its value changes."]

   [:p "Let's demonstrate that with a simple example:"]
   [demo-component {:comp counting-component
                    :defs [:ns :click-count :counting-component]}]

   [:p "Sometimes you may want to maintain state locally in a
   component. That is easy to do with an " [:code "atom"] " as well."]

   [:p "Here is an example of that, where we call "
   [:code "setTimeout"] " every time the component is rendered to
   update a counter:"]
   
   [demo-component {:comp timer-component
                    :defs [:timer-component]}]
   
   [:p "The previous example also uses another feature of Cloact: a component
   function can return another function, that is used to do the actual
   rendering. It is called with the same arguments as any other
   component function. This allows you to perform some setup of newly
   created components, without resorting to React's lifecycle
   events."]

   [:p "By simply passing atoms around you can share state management
   between components, like this:"]
   [demo-component {:comp shared-state
                    :defs [:ns :atom-input :shared-state]}]])

(defn essential-api []
  [:div.demo-text
   [:h2 "Essential API"]

   [:p "Cloact supports most of React's API, but there is really only
   one entry-point that is necessary for most applications: "
    [:code "cloact.core/render-component"] "."]

   [:p "It takes too arguments: a component, and a DOM node. For
   example, splashing the very first example all over the page would
   look like this:"]

   [demo-component {:defs [:ns :simple-component :render-simple]}]])

(defn performance []
  [:div.demo-text
   [:h2 "Performance"]

   [:p "Something about performance..."]])

(defn bmi-demo []
  [:div.demo-text
   [:h2 "Putting it all together"]
   
   [:p "Here is a slightly less contrived example: a simple BMI
   calculator."]

   [:p "Data is kept in a single " [:code "cloact.core/atom"] ": a map
   with height, weight and BMI as keys."]

   [demo-component {:comp bmi-component
                    :defs [:ns :calc-bmi :bmi-data :set-bmi :slider
                           :bmi-component]}]])

(defn test-results []
  [:div.demo-text
   [:h2 "Test results"]
   [runtests/test-output]])

(defn complete-simple-demo []
  [:div.demo-text
   [:h2 "Complete demo"]

   [:p "Cloact comes with a couple of complete examples, with
   Leiningen project files and everything. Here's one of them in
   action:"]
   
   [demo-component {:comp simpleexample/simple-example
                    :src (get-source "simpleexample.cljs")}]])

(defn todomvc-demo []
  [:div.demo-text
   [:h2 "Todomvc"]

   [:p "The obligatory todo list looks roughly like this in
   Cloact (cheating a little bit by skipping routing and
   persistence):"]
   
   [demo-component {:comp todomvc/todo-app
                    :src (get-source "todomvc.cljs")}]])

(defn github-badge []
  [:a.github-badge
   {:href "https://github.com/holmsand/cloact"}
   [:img {:style {:position "absolute" :top 0 :left 0 :border 0}
          :src "https://s3.amazonaws.com/github/ribbons/forkme_left_orange_ff7600.png"
          :alt "Fork me on GitHub"}]])

(defn demo []
  [:div
   [:div.test-output-mini
    [runtests/test-output-mini]]
   [:div.cloact-demo
    [:h1 "Cloact: Simple and fast UI for ClojureScript"]
    [intro]
    [managing-state]
    [essential-api]
    [performance]
    [bmi-demo]
    [test-results]
    [complete-simple-demo]
    [todomvc-demo]
    [github-badge]]])

(defn ^:export mountdemo []
  (cloact/render-component [demo] (.-body js/document)))

(defn ^:export genpage []
  (cloact/render-component-to-string [demo]))
